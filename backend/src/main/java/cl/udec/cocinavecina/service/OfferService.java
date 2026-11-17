package cl.udec.cocinavecina.service;

import cl.udec.cocinavecina.components.FieldEncryptor;
import cl.udec.cocinavecina.components.NominatimGeocoder;
import cl.udec.cocinavecina.dto.*;
import cl.udec.cocinavecina.exception.ApiException;
import cl.udec.cocinavecina.model.*;
import cl.udec.cocinavecina.projection.OfferProjection;
import cl.udec.cocinavecina.repository.*;
import cl.udec.cocinavecina.rules.Obfuscation;
import cl.udec.cocinavecina.rules.OfferRules;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OfferService {
    private final OfferRepository offers;
    private final OfferAllergenRepository offerAllergens;
    private final PickUpPointRepository pickUpPoints;
    private final AppUserRepository users;
    private final PurchaseRequestRepository purchaseRequests;
    private final NominatimGeocoder geocoder;
    private final FieldEncryptor encryptor;
    private final AuditService audit;
    private final String photosPublicUrl;

    public OfferService(
        OfferRepository offers,
        OfferAllergenRepository offerAllergens,
        PickUpPointRepository pickUpPoints,
        AppUserRepository users,
        PurchaseRequestRepository purchaseRequests,
        NominatimGeocoder geocoder,
        FieldEncryptor encryptor,
        AuditService audit,
        @Value("${cocinavecina.photos.public-url}") String photosPublicUrl
    ) {
        this.offers = offers;
        this.offerAllergens = offerAllergens;
        this.pickUpPoints = pickUpPoints;
        this.users = users;
        this.purchaseRequests = purchaseRequests;
        this.geocoder = geocoder;
        this.encryptor = encryptor;
        this.audit = audit;
        this.photosPublicUrl = photosPublicUrl;
    }

    @Transactional
    public PickUpPointView createPickUpPoint(Long sellerId, NewPickUpPointRequest request) {
        final AppUser seller = requireActive(sellerId);
        final NominatimGeocoder.Coordinates coordinates = request.latitude() != null && request.longitude() != null
            ? new NominatimGeocoder.Coordinates(request.latitude(), request.longitude())
            : geocoder.geocode(request.address())
                .orElseThrow(() -> ApiException.rule("RF-2", "No se encontró la dirección"));
        final Obfuscation.Cell cell = Obfuscation.cellCenter(coordinates.latitude(), coordinates.longitude());

        final PickupPoint point = pickUpPoints.save(PickupPoint.builder()
            .seller(seller)
            .alias(request.alias().trim())
            .encryptedAddress(encryptor.encrypt(request.address()))
            .encryptedInstructions(encryptor.encrypt(request.instructions()))
            .encryptedLat(encryptor.encrypt(Double.toString(coordinates.latitude())))
            .encryptedLon(encryptor.encrypt(Double.toString(coordinates.longitude())))
            .cellLat(cell.latitude())
            .cellLon(cell.longitude())
            .createdAt(OffsetDateTime.now())
            .build());
        return new PickUpPointView(point.getId(), point.getAlias(), point.getCellLat(), point.getCellLon());
    }

    @Transactional(readOnly = true)
    public List<PickUpPointView> myPickUpPoints(Long sellerId) {
        return pickUpPoints.findViewsBySeller(sellerId).stream()
            .map(p -> new PickUpPointView(p.getId(), p.getAlias(), p.getCellLat(), p.getCellLon()))
            .toList();
    }

    @Transactional
    public OfferView publish(Long sellerId, NewOfferRequest request) {
        final AppUser seller = requireActive(sellerId);
        final PickupPoint point = pickUpPoints.findByIdAndSeller_Id(request.pickupPointId(), sellerId)
            .orElseThrow(() -> ApiException.forbidden("El punto de retiro no pertenece al vendedor"));
        final Instant now = Instant.now();
        OfferRules.validateNoExcludedIngredients(request.excludedIngredients());
        OfferRules.validateAllergens(request.allergens());
        OfferRules.validatePrices(request.basePrice(), request.minPrice());
        OfferRules.validatePortions(request.portions());
        OfferRules.validateTimes(now, request.preparedAt(), request.expiresAt());

        final Offer offer = offers.save(Offer.builder()
            .seller(seller)
            .pickupPoint(point)
            .title(request.title().trim())
            .description(request.description())
            .category(request.category())
            .photoKey(request.photoKey())
            .basePrice(request.basePrice())
            .minPrice(request.minPrice())
            .totalPortions(request.portions())
            .availablePortions(request.portions())
            .preparedAt(utc(request.preparedAt()))
            .publishedAt(utc(now))
            .expiresAt(utc(request.expiresAt()))
            .status(OfferStatus.ACTIVE)
            .build());
        offerAllergens.saveAll(request.allergens().stream()
            .map(a -> OfferAllergen.builder()
                .id(new OfferAllergenId(offer.getId(), a.name()))
                .offer(offer)
                .build())
            .toList());

        audit.log(sellerId, "OFFER_PUBLISHED", "offer", offer.getId());
        return detail(offer.getId());
    }

    @Transactional
    public void close(Long sellerId, Long offerId) {
        final Offer offer = offers.findById(offerId)
            .orElseThrow(() -> ApiException.notFound("No existe la oferta " + offerId));
        if (!offer.getSeller().getId().equals(sellerId)) {
            throw ApiException.forbidden("Solo el vendedor puede cerrar su oferta");
        }
        if (offer.getStatus() == OfferStatus.ACTIVE || offer.getStatus() == OfferStatus.SOLD_OUT) {
            offer.setStatus(OfferStatus.CLOSED);
            offer.setClosedAt(OffsetDateTime.now());
            purchaseRequests.expireOpenByOffer(offerId, "Oferta cerrada");
        }
    }

    @Transactional(readOnly = true)
    public List<NearbyOfferView> nearby(
        double lat,
        double lon,
        int radius,
        Category category,
        List<Allergen> excludeAllergens,
        NearbySort sort
    ) {
        OfferRules.validateCoordinates(lat, lon);
        OfferRules.validateRadius(radius);
        final String categoryFilter = category == null ? "" : category.name();
        final String excluded = excludeAllergens == null
            ? ""
            : excludeAllergens.stream().map(Enum::name).collect(Collectors.joining(","));
        return offers.findNearby(lat, lon, radius, categoryFilter, excluded, sort.name()).stream()
            .map(p -> new NearbyOfferView(
                p.getId(),
                p.getTitle(),
                p.getCategory(),
                photoUrl(p.getPhotoKey()),
                p.getBasePrice(),
                p.getMinPrice(),
                p.getAvailablePortions(),
                p.getExpiresAt(),
                p.getCellLat(),
                p.getCellLon(),
                Obfuscation.roundDistance(p.getDistance())
            ))
            .toList();
    }

    @Transactional(readOnly = true)
    public OfferView detail(Long offerId) {
        return offers.findViewById(offerId)
            .map(this::toView)
            .orElseThrow(() -> ApiException.notFound("No existe la oferta " + offerId));
    }

    @Transactional(readOnly = true)
    public List<OfferView> bySeller(Long sellerId) {
        return offers.findViewsBySeller(sellerId).stream().map(this::toView).toList();
    }

    private OfferView toView(OfferProjection p) {
        return new OfferView(
            p.getId(),
            p.getSellerId(),
            p.getSeller(),
            p.getTitle(),
            p.getDescription(),
            p.getCategory(),
            photoUrl(p.getPhotoKey()),
            p.getBasePrice(),
            p.getMinPrice(),
            p.getAvailablePortions(),
            allergens(p.getAllergens()),
            p.getPreparedAt(),
            p.getExpiresAt(),
            p.getStatus(),
            p.getCellLat(),
            p.getCellLon()
        );
    }

    private AppUser requireActive(Long userId) {
        final AppUser user = users.findById(userId)
            .orElseThrow(() -> ApiException.notFound("No existe el usuario " + userId));
        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw ApiException.forbidden("La cuenta debe estar verificada y activa");
        }
        return user;
    }

    private String photoUrl(String photoKey) {
        return photoKey == null ? null : photosPublicUrl + "/" + photoKey;
    }

    private static Set<Allergen> allergens(String csv) {
        if (csv == null || csv.isBlank()) {
            return EnumSet.noneOf(Allergen.class);
        }
        return Arrays.stream(csv.split(","))
            .map(Allergen::valueOf)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(Allergen.class)));
    }

    private static OffsetDateTime utc(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}

package cl.udec.cocinavecina.service;

import cl.udec.cocinavecina.components.FieldEncryptor;
import cl.udec.cocinavecina.dto.AccountExport;
import cl.udec.cocinavecina.exception.ApiException;
import cl.udec.cocinavecina.model.AccountStatus;
import cl.udec.cocinavecina.model.AppUser;
import cl.udec.cocinavecina.repository.AppUserRepository;
import cl.udec.cocinavecina.repository.OfferRepository;
import cl.udec.cocinavecina.repository.OrdersRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AppUserService {
    private final AppUserRepository users;
    private final OfferRepository offers;
    private final OrdersRepository orders;
    private final OfferService offerService;
    private final FieldEncryptor encryptor;
    private final AuditService audit;

    public AppUserService(AppUserRepository users,
                          OfferRepository offers,
                          OrdersRepository orders,
                          OfferService offerService,
                          FieldEncryptor encryptor,
                          AuditService audit) {
        this.users = users;
        this.offers = offers;
        this.orders = orders;
        this.offerService = offerService;
        this.encryptor = encryptor;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public AccountExport export(Long userId) {
        AppUser user = get(userId);
        AccountExport.Account account = new AccountExport.Account(
            user.getId(),
            user.getEmail(),
            user.getDisplayName(),
            encryptor.decrypt(user.getEncryptedPhone()),
            user.getRole(),
            user.getStatus(),
            Boolean.TRUE.equals(user.getAdultDeclared()),
            user.getCreatedAt().toInstant(),
            user.getVerifiedAt() == null ? null : user.getVerifiedAt().toInstant());
        List<AccountExport.OrderSummary> orderSummaries = orders.findSummariesByUser(userId).stream()
            .map(o -> new AccountExport.OrderSummary(
                o.getId(),
                o.getOfferId(),
                o.getOfferTitle(),
                o.getSide(),
                o.getAgreedPrice(),
                o.getPortions(),
                o.getStatus(),
                o.getAcceptedAt()))
            .toList();
        return new AccountExport(account, offerService.bySeller(userId), orderSummaries);
    }

    @Transactional
    public void delete(Long userId) {
        AppUser user = get(userId);
        if (orders.hasActiveOrders(userId)) {
            throw ApiException.conflict("Tienes pedidos en curso; podrás eliminar la cuenta cuando terminen");
        }
        offers.closeAllBySeller(userId);
        user.setEmail("deleted-" + user.getId() + "@cocinavecina.invalid");
        user.setDisplayName("Usuario eliminado " + user.getId());
        user.setPasswordHash("!");
        user.setEncryptedPhone(null);
        user.setStatus(AccountStatus.DELETED);
        audit.log(userId, "ACCOUNT_DELETED", "user", userId);
    }

    private AppUser get(Long userId) {
        return users.findById(userId)
            .orElseThrow(() -> ApiException.notFound("No existe el usuario " + userId));
    }
}

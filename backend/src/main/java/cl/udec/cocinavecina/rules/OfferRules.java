package cl.udec.cocinavecina.rules;

import cl.udec.cocinavecina.exception.ApiException;
import cl.udec.cocinavecina.model.Allergen;
import cl.udec.cocinavecina.model.RiskIngredient;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

public final class OfferRules {
    public static final Duration MIN_DURATION = Duration.ofMinutes(20);
    public static final Duration MAX_SINCE_PREPARATION = Duration.ofMinutes(210);
    public static final Set<Integer> RADII = Set.of(500, 1500, 3000, 5000);

    private OfferRules() {
    }

    public static void validateTimes(Instant now, Instant preparedAt, Instant expiresAt) {
        if (preparedAt.isAfter(now)) {
            throw ApiException.rule("RB-1", "La hora de preparación no puede ser futura");
        }
        if (Duration.between(now, expiresAt).compareTo(MIN_DURATION) < 0) {
            throw ApiException.rule("RB-1", "La oferta debe durar al menos 20 minutos");
        }
        if (expiresAt.isAfter(preparedAt.plus(MAX_SINCE_PREPARATION))) {
            throw ApiException.rule(
                "RB-1",
                "La oferta debe terminar a más tardar 3 h 30 min después de la preparación"
            );
        }
    }

    public static void validatePrices(int basePrice, int minPrice) {
        if (basePrice <= 0 || basePrice % 100 != 0 || minPrice % 100 != 0) {
            throw ApiException.rule("RB-2", "Los precios deben ser múltiplos de $100");
        }
        if (minPrice > basePrice || minPrice * 2 < basePrice) {
            throw ApiException.rule("RB-2", "El precio mínimo debe estar entre el 50 % y el 100 % del precio base");
        }
    }

    public static void validatePortions(int portions) {
        if (portions < 1 || portions > 50) {
            throw ApiException.rule("RF-2", "Las porciones deben estar entre 1 y 50");
        }
    }

    public static void validateAllergens(Set<Allergen> allergens) {
        if (allergens == null || allergens.isEmpty()) {
            throw ApiException.rule("RF-2", "Debe declarar los alérgenos o «ninguno conocido»");
        }
        if (allergens.contains(Allergen.NONE_KNOWN) && allergens.size() > 1) {
            throw ApiException.rule("RF-2", "«Ninguno conocido» no se combina con otros alérgenos");
        }
    }

    public static void validateNoExcludedIngredients(Set<RiskIngredient> declared) {
        if (declared != null && !declared.isEmpty()) {
            throw ApiException.rule("RB-7", "La preparación contiene ingredientes de una categoría excluida");
        }
    }

    public static void validateRadius(int radius) {
        if (!RADII.contains(radius)) {
            throw ApiException.rule("RF-4", "Radio permitido: 500, 1500, 3000 o 5000 m");
        }
    }

    public static void validateCoordinates(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw ApiException.badRequest("Coordenadas fuera de rango");
        }
    }
}

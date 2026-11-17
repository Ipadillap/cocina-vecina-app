package cl.udec.cocinavecina.rules;

import cl.udec.cocinavecina.exception.ApiException;

public final class NegotiationRules {
    public static final int MAX_ROUNDS = 3;
    public static final int MAX_PORTIONS = 5;
    public static final int PRICE_STEP = 100;

    private NegotiationRules() {
    }

    public static void validatePortions(int portions, int availablePortions) {
        if (portions < 1 || portions > MAX_PORTIONS) {
            throw ApiException.rule("RB-4", "Puedes pedir entre 1 y " + MAX_PORTIONS + " porciones por solicitud");
        }
        if (portions > availablePortions) {
            throw ApiException.rule("RB-4", "Solo quedan " + availablePortions + " porciones disponibles");
        }
    }

    public static void validateBuyerProposal(int unitPrice, int minPrice, int basePrice) {
        validateStep(unitPrice);
        if (unitPrice < minPrice || unitPrice > basePrice) {
            throw ApiException.rule(
                "RB-2",
                "El precio propuesto debe estar entre $" + minPrice + " y $" + basePrice
            );
        }
    }

    public static void validateCounteroffer(int unitPrice, int currentProposal, int basePrice) {
        validateStep(unitPrice);
        if (unitPrice < currentProposal || unitPrice > basePrice) {
            throw ApiException.rule(
                "RB-2",
                "La contraoferta debe estar entre $" + currentProposal + " y $" + basePrice
            );
        }
    }

    public static void validateRoundAvailable(int rounds) {
        if (rounds >= MAX_ROUNDS) {
            throw ApiException.rule(
                "RB-3",
                "Ya se completaron las " + MAX_ROUNDS + " rondas de negociación; solo puedes aceptar o rechazar"
            );
        }
    }

    private static void validateStep(int unitPrice) {
        if (unitPrice <= 0 || unitPrice % PRICE_STEP != 0) {
            throw ApiException.rule("RB-2", "Los precios deben ser múltiplos de $" + PRICE_STEP);
        }
    }
}

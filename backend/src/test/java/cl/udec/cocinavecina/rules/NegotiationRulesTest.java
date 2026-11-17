package cl.udec.cocinavecina.rules;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cl.udec.cocinavecina.exception.ApiException;
import org.junit.jupiter.api.Test;

/** RB-2, RB-3 y RB-4: reglas de la negociación. */
class NegotiationRulesTest {

    private static void assertRule(String rule, Runnable action) {
        final ApiException ex = assertThrows(ApiException.class, action::run);
        assertEquals(rule, ex.getResponse().title());
        assertEquals(422, ex.getResponse().status());
    }

    // ---------- RB-4: porciones por solicitud ----------

    @Test
    void acceptsPortionsWithinLimitAndStock() {
        assertDoesNotThrow(() -> NegotiationRules.validatePortions(1, 10));
        assertDoesNotThrow(() -> NegotiationRules.validatePortions(5, 5));
    }

    @Test
    void rejectsZeroOrMoreThanFivePortions() {
        assertRule("RB-4", () -> NegotiationRules.validatePortions(0, 10));
        assertRule("RB-4", () -> NegotiationRules.validatePortions(6, 10));
    }

    @Test
    void rejectsMorePortionsThanAvailable() {
        assertRule("RB-4", () -> NegotiationRules.validatePortions(3, 2));
    }

    // ---------- RB-2: propuesta del comprador ----------

    @Test
    void acceptsBuyerProposalInsideRange() {
        assertDoesNotThrow(() -> NegotiationRules.validateBuyerProposal(2500, 2000, 3000));
        assertDoesNotThrow(() -> NegotiationRules.validateBuyerProposal(2000, 2000, 3000));
        assertDoesNotThrow(() -> NegotiationRules.validateBuyerProposal(3000, 2000, 3000));
    }

    @Test
    void rejectsBuyerProposalOutsideRange() {
        assertRule("RB-2", () -> NegotiationRules.validateBuyerProposal(1900, 2000, 3000));
        assertRule("RB-2", () -> NegotiationRules.validateBuyerProposal(3100, 2000, 3000));
    }

    @Test
    void rejectsPricesThatAreNotMultiplesOf100() {
        assertRule("RB-2", () -> NegotiationRules.validateBuyerProposal(2550, 2000, 3000));
        assertRule("RB-2", () -> NegotiationRules.validateCounteroffer(2750, 2500, 3000));
    }

    @Test
    void rejectsNonPositivePrice() {
        assertRule("RB-2", () -> NegotiationRules.validateBuyerProposal(0, 0, 3000));
    }

    // ---------- RB-2: contraoferta del vendedor ----------

    @Test
    void acceptsCounterofferBetweenProposalAndBase() {
        assertDoesNotThrow(() -> NegotiationRules.validateCounteroffer(2700, 2500, 3000));
        assertDoesNotThrow(() -> NegotiationRules.validateCounteroffer(2500, 2500, 3000));
    }

    @Test
    void rejectsCounterofferBelowCurrentProposal() {
        assertRule("RB-2", () -> NegotiationRules.validateCounteroffer(2400, 2500, 3000));
    }

    @Test
    void rejectsCounterofferAboveBasePrice() {
        assertRule("RB-2", () -> NegotiationRules.validateCounteroffer(3100, 2500, 3000));
    }

    // ---------- RB-3: máximo 3 rondas ----------

    @Test
    void allowsRoundsBeforeTheThird() {
        assertDoesNotThrow(() -> NegotiationRules.validateRoundAvailable(0));
        assertDoesNotThrow(() -> NegotiationRules.validateRoundAvailable(2));
    }

    @Test
    void blocksAFourthRound() {
        assertRule("RB-3", () -> NegotiationRules.validateRoundAvailable(3));
        assertRule("RB-3", () -> NegotiationRules.validateRoundAvailable(4));
    }
}

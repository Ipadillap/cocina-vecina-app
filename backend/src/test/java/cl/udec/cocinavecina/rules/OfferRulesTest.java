package cl.udec.cocinavecina.rules;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cl.udec.cocinavecina.exception.ApiException;
import cl.udec.cocinavecina.model.Allergen;
import cl.udec.cocinavecina.model.RiskIngredient;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** RB-1, RB-2, RB-7, RF-2 y RF-4: reglas de publicación y búsqueda de ofertas. */
class OfferRulesTest {
    private static final Instant NOW = Instant.parse("2026-11-17T12:00:00Z");

    private static void assertRule(String rule, Runnable action) {
        final ApiException ex = assertThrows(ApiException.class, action::run);
        assertEquals(rule, ex.getResponse().title());
        assertEquals(422, ex.getResponse().status());
    }

    // ---------- RB-1: tiempos ----------

    @Test
    void acceptsOfferWithinTimeWindow() {
        assertDoesNotThrow(() -> OfferRules.validateTimes(
            NOW, NOW.minus(Duration.ofMinutes(30)), NOW.plus(Duration.ofHours(2))));
    }

    @Test
    void rejectsFuturePreparationTime() {
        assertRule("RB-1", () -> OfferRules.validateTimes(
            NOW, NOW.plus(Duration.ofMinutes(1)), NOW.plus(Duration.ofHours(1))));
    }

    @Test
    void rejectsOfferShorterThan20Minutes() {
        assertRule("RB-1", () -> OfferRules.validateTimes(
            NOW, NOW.minus(Duration.ofMinutes(10)), NOW.plus(Duration.ofMinutes(19))));
    }

    @Test
    void acceptsOfferOfExactly20Minutes() {
        assertDoesNotThrow(() -> OfferRules.validateTimes(
            NOW, NOW.minus(Duration.ofMinutes(10)), NOW.plus(Duration.ofMinutes(20))));
    }

    @Test
    void rejectsOfferEndingAfter3h30FromPreparation() {
        final Instant prepared = NOW.minus(Duration.ofHours(1));
        assertRule("RB-1", () -> OfferRules.validateTimes(
            NOW, prepared, prepared.plus(Duration.ofMinutes(211))));
    }

    @Test
    void acceptsOfferEndingExactly3h30FromPreparation() {
        final Instant prepared = NOW.minus(Duration.ofHours(1));
        assertDoesNotThrow(() -> OfferRules.validateTimes(
            NOW, prepared, prepared.plus(Duration.ofMinutes(210))));
    }

    // ---------- RB-2: precios ----------

    @Test
    void acceptsValidPrices() {
        assertDoesNotThrow(() -> OfferRules.validatePrices(3000, 2000));
    }

    @Test
    void acceptsMinPriceAtExactly50Percent() {
        assertDoesNotThrow(() -> OfferRules.validatePrices(3000, 1500));
    }

    @Test
    void acceptsMinPriceEqualToBasePrice() {
        assertDoesNotThrow(() -> OfferRules.validatePrices(3000, 3000));
    }

    @Test
    void rejectsPricesNotMultipleOf100() {
        assertRule("RB-2", () -> OfferRules.validatePrices(3050, 2000));
        assertRule("RB-2", () -> OfferRules.validatePrices(3000, 2050));
    }

    @Test
    void rejectsNonPositiveBasePrice() {
        assertRule("RB-2", () -> OfferRules.validatePrices(0, 0));
    }

    @Test
    void rejectsMinPriceBelow50Percent() {
        assertRule("RB-2", () -> OfferRules.validatePrices(3000, 1400));
    }

    @Test
    void rejectsMinPriceAboveBasePrice() {
        assertRule("RB-2", () -> OfferRules.validatePrices(3000, 3100));
    }

    // ---------- RF-2: porciones y alérgenos ----------

    @Test
    void acceptsPortionLimits() {
        assertDoesNotThrow(() -> OfferRules.validatePortions(1));
        assertDoesNotThrow(() -> OfferRules.validatePortions(50));
    }

    @Test
    void rejectsPortionsOutOfRange() {
        assertRule("RF-2", () -> OfferRules.validatePortions(0));
        assertRule("RF-2", () -> OfferRules.validatePortions(51));
    }

    @Test
    void requiresAllergenDeclaration() {
        assertRule("RF-2", () -> OfferRules.validateAllergens(null));
        assertRule("RF-2", () -> OfferRules.validateAllergens(Set.of()));
    }

    @Test
    void acceptsNoneKnownAlone() {
        assertDoesNotThrow(() -> OfferRules.validateAllergens(EnumSet.of(Allergen.NONE_KNOWN)));
    }

    @Test
    void rejectsNoneKnownCombinedWithOtherAllergens() {
        assertRule("RF-2", () -> OfferRules.validateAllergens(EnumSet.of(Allergen.NONE_KNOWN, Allergen.GLUTEN)));
    }

    @Test
    void acceptsSeveralRealAllergens() {
        assertDoesNotThrow(() -> OfferRules.validateAllergens(EnumSet.of(Allergen.GLUTEN, Allergen.MILK)));
    }

    // ---------- RB-7: ingredientes excluidos ----------

    @Test
    void acceptsNoRiskIngredients() {
        assertDoesNotThrow(() -> OfferRules.validateNoExcludedIngredients(null));
        assertDoesNotThrow(() -> OfferRules.validateNoExcludedIngredients(Set.of()));
    }

    @Test
    void rejectsEveryRiskIngredient() {
        for (RiskIngredient ingredient : RiskIngredient.values()) {
            assertRule("RB-7", () -> OfferRules.validateNoExcludedIngredients(EnumSet.of(ingredient)));
        }
    }

    // ---------- RF-4: radio y coordenadas ----------

    @Test
    void acceptsAllowedRadii() {
        for (int radius : new int[] {500, 1500, 3000, 5000}) {
            assertDoesNotThrow(() -> OfferRules.validateRadius(radius));
        }
    }

    @Test
    void rejectsOtherRadii() {
        assertRule("RF-4", () -> OfferRules.validateRadius(1000));
        assertRule("RF-4", () -> OfferRules.validateRadius(10000));
    }

    @Test
    void validatesCoordinateRange() {
        assertDoesNotThrow(() -> OfferRules.validateCoordinates(-36.827, -73.050));
        final ApiException ex = assertThrows(ApiException.class, () -> OfferRules.validateCoordinates(-91, 0));
        assertEquals(400, ex.getResponse().status());
        assertThrows(ApiException.class, () -> OfferRules.validateCoordinates(0, 181));
    }
}

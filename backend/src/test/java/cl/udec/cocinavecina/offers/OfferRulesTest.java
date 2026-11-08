package cl.udec.cocinavecina.offers;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.udec.cocinavecina.shared.error.BusinessRuleException;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OfferRulesTest {

    private final Instant now = Instant.parse("2026-11-10T16:00:00Z");

    @Test
    void rb1AcceptsTwoHourOfferPreparedHalfHourAgo() {
        assertThatCode(() -> OfferRules.validateTimes(
                        now, now.minus(Duration.ofMinutes(30)), now.plus(Duration.ofHours(2))))
                .doesNotThrowAnyException();
    }

    @Test
    void rb1RejectsLessThan20Minutes() {
        assertThatThrownBy(() -> OfferRules.validateTimes(now, now, now.plus(Duration.ofMinutes(19))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("20 minutos");
    }

    @Test
    void rb1RejectsEndingMoreThan3h30AfterPreparation() {
        Instant preparedAt = now.minus(Duration.ofHours(2));
        assertThatThrownBy(() -> OfferRules.validateTimes(now, preparedAt, now.plus(Duration.ofMinutes(91))))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rb2MinimumMustBeBetween50And100Percent() {
        assertThatCode(() -> OfferRules.validatePrices(3000, 1500)).doesNotThrowAnyException();
        assertThatThrownBy(() -> OfferRules.validatePrices(3000, 1400)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> OfferRules.validatePrices(3000, 3100)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> OfferRules.validatePrices(3050, 2000)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void noneKnownCannotBeCombined() {
        assertThatThrownBy(() -> OfferRules.validateAllergens(EnumSet.of(Allergen.NONE_KNOWN, Allergen.GLUTEN)))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> OfferRules.validateAllergens(Set.of())).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rb7BlocksExcludedIngredients() {
        assertThatThrownBy(() -> OfferRules.validateNoExcludedIngredients(Set.of(RiskIngredient.RAW_OR_UNDERCOOKED_EGG)))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(e -> ((BusinessRuleException) e).rule())
                .isEqualTo("RB-7");
    }
}

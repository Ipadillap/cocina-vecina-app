package cl.udec.cocinavecina.negotiation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.udec.cocinavecina.shared.error.BusinessRuleException;
import org.junit.jupiter.api.Test;

class NegotiationRulesTest {

    @Test
    void tc04ProposalBelowMinimumIsRejectedWithRange() {
        assertThatThrownBy(() -> NegotiationRules.validateBuyerProposal(2300, 2400, 3000))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("$2400")
                .hasMessageContaining("$3000");
    }

    @Test
    void counterofferGoesFromCurrentProposalToBase() {
        assertThatCode(() -> NegotiationRules.validateCounteroffer(2800, 2600, 3000)).doesNotThrowAnyException();
        assertThatThrownBy(() -> NegotiationRules.validateCounteroffer(2500, 2600, 3000))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rb3AfterThreeRoundsOnlyAcceptOrReject() {
        assertThatCode(() -> NegotiationRules.validateRoundAvailable(2)).doesNotThrowAnyException();
        assertThatThrownBy(() -> NegotiationRules.validateRoundAvailable(3))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("3 rondas");
    }

    @Test
    void rb4AtMostFivePortionsAndNoMoreThanStock() {
        assertThatCode(() -> NegotiationRules.validatePortions(5, 8)).doesNotThrowAnyException();
        assertThatThrownBy(() -> NegotiationRules.validatePortions(6, 8)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> NegotiationRules.validatePortions(3, 2)).isInstanceOf(BusinessRuleException.class);
    }
}

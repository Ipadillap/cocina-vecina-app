package cl.udec.cocinavecina.ui.publish

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class PublishRulesTest {
    private val now = Instant.parse("2026-11-16T15:00:00Z")

    @Test
    fun lowestAcceptedPriceIsHalfRoundedUpToHundreds() {
        assertEquals(1800, PublishRules.lowestAcceptedPrice(3500))
        assertEquals(300, PublishRules.lowestAcceptedPrice(500))
    }

    @Test
    fun preparedTimeCannotBeInTheFutureOrOlderThanThreeHours() {
        assertEquals(now, PublishRules.clampPrepared(now.plus(10, ChronoUnit.MINUTES), now))
        assertEquals(now.minus(180, ChronoUnit.MINUTES), PublishRules.clampPrepared(now.minus(5, ChronoUnit.HOURS), now))
    }

    @Test
    fun saleEndIsBetweenTheMinimumSaleAndThreeAndAHalfHoursAfterPreparing() {
        val prepared = now.minus(30, ChronoUnit.MINUTES)
        assertEquals(now.plus(20, ChronoUnit.MINUTES), PublishRules.clampSaleEnd(now, prepared, now))
        assertEquals(prepared.plus(210, ChronoUnit.MINUTES), PublishRules.clampSaleEnd(now.plus(6, ChronoUnit.HOURS), prepared, now))
    }

    @Test
    fun pickupWindowStartsWhenTheSaleEnds() {
        assertEquals(now.plus(30, ChronoUnit.MINUTES), PublishRules.pickupEnd(now))
    }
}

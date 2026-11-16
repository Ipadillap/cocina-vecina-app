package cl.udec.cocinavecina.data.fake

import cl.udec.cocinavecina.data.network.OfferUnderReview
import cl.udec.cocinavecina.data.network.ReportView
import java.time.Instant
import java.time.temporal.ChronoUnit

data class FakeUser(
    val id: Long,
    val email: String,
    val displayName: String,
    val password: String,
    val role: String,
    val verified: Boolean,
    val phone: String? = null,
    val termsAcceptedAt: Instant = Instant.parse("2026-10-08T15:00:00Z"),
)

data class FakeOffer(
    val id: Long,
    val sellerId: Long,
    val pickupPointId: Long,
    val title: String,
    val description: String?,
    val category: String,
    val basePrice: Int,
    val minPrice: Int,
    val availablePortions: Int,
    val allergens: List<String>,
    val preparedAt: Instant,
    val expiresAt: Instant,
    val status: String,
    val totalPortions: Int = availablePortions,
)

data class FakePickupPoint(
    val id: Long,
    val sellerId: Long,
    val alias: String,
    val address: String,
    val instructions: String?,
    val latitude: Double,
    val longitude: Double,
)

object FakeSeed {
    const val PASSWORD = "una-clave-larga"
    const val BUYER_ID = 1L
    const val SELLER_ID = 2L
    const val MODERATOR_ID = 3L
    const val SECOND_SELLER_ID = 5L
    const val SECOND_BUYER_ID = 6L

    fun users(): List<FakeUser> =
        listOf(
            FakeUser(BUYER_ID, "comprador@example.com", "Camila", PASSWORD, "BUYER", verified = true, phone = "+56911110000"),
            FakeUser(SELLER_ID, "vendedora@example.com", "Ana", PASSWORD, "SELLER", verified = true, phone = "+56911112222"),
            FakeUser(MODERATOR_ID, "moderador@example.com", "Equipo CocinaVecina", PASSWORD, "MODERATOR", verified = true),
            FakeUser(4L, "sinverificar@example.com", "Pedro", PASSWORD, "BUYER", verified = false),
            FakeUser(SECOND_SELLER_ID, "vendedora2@example.com", "Rosa", PASSWORD, "SELLER", verified = true, phone = "+56922223333"),
            FakeUser(SECOND_BUYER_ID, "comprador2@example.com", "Tomás", PASSWORD, "BUYER", verified = true),
        )

    fun pickupPoints(): List<FakePickupPoint> =
        listOf(
            FakePickupPoint(1L, SELLER_ID, "Casa", "Barros Arana 1234, Concepción", "Timbre del depto 3B", -36.8268, -73.0497),
            FakePickupPoint(2L, SECOND_SELLER_ID, "Depto", "Janequeo 456, Concepción", "Conserjería, depto 1204", -36.8305, -73.0390),
        )

    fun offers(now: Instant): List<FakeOffer> =
        listOf(
            FakeOffer(
                1L, SELLER_ID, 1L, "Pastel de choclo", "Con pino de vacuno, pollo y aceitunas.", "MAIN_COURSE",
                4500, 3800, 4, listOf("MILK", "EGG"), now.minus(1, ChronoUnit.HOURS), now.plus(3, ChronoUnit.HOURS), "ACTIVE",
            ),
            FakeOffer(
                2L, SELLER_ID, 1L, "Empanadas de pino", "Horneadas, porción de dos unidades.", "EMPANADAS",
                3500, 3000, 6, listOf("GLUTEN", "EGG"), now.minus(30, ChronoUnit.MINUTES), now.plus(2, ChronoUnit.HOURS), "ACTIVE",
                totalPortions = 8,
            ),
            FakeOffer(
                3L, SELLER_ID, 1L, "Crema de zapallo", null, "SOUPS_AND_CREAMS",
                2500, 2000, 3, listOf("MILK"), now.minus(20, ChronoUnit.MINUTES), now.plus(90, ChronoUnit.MINUTES), "ACTIVE",
            ),
            FakeOffer(
                4L, SELLER_ID, 1L, "Pan amasado", "Bolsa de cuatro unidades, recién salido del horno.", "HOMEMADE_BREAD",
                2000, 1500, 8, listOf("GLUTEN"), now.minus(10, ChronoUnit.MINUTES), now.plus(5, ChronoUnit.HOURS), "ACTIVE",
            ),
            FakeOffer(
                6L, SECOND_SELLER_ID, 2L, "Lentejas con longaniza", "Porción generosa, ideal para el almuerzo.", "MAIN_COURSE",
                3800, 3200, 5, emptyList(), now.minus(40, ChronoUnit.MINUTES), now.plus(45, ChronoUnit.MINUTES), "ACTIVE",
            ),
            FakeOffer(
                7L, SECOND_SELLER_ID, 2L, "Ensalada chilena", null, "SALADS",
                1800, 1500, 3, emptyList(), now.minus(15, ChronoUnit.MINUTES), now.plus(150, ChronoUnit.MINUTES), "ACTIVE",
            ),
            FakeOffer(
                5L, SELLER_ID, 1L, "Kuchen de murta", null, "PASTRY",
                3000, 2500, 2, listOf("GLUTEN", "MILK", "EGG"), now.minus(2, ChronoUnit.HOURS), now.plus(4, ChronoUnit.HOURS), "HIDDEN",
            ),
        )

    fun hiddenOffers(now: Instant): List<OfferUnderReview> =
        listOf(
            OfferUnderReview(
                offerId = 5L,
                title = "Kuchen de murta",
                sellerId = SELLER_ID,
                sellerReportCount = 3,
                reports =
                    listOf(
                        ReportView("HEALTH_RISK", "Llegó con mal olor", now.minus(40, ChronoUnit.MINUTES).toString()),
                        ReportView("OTHER", null, now.minus(25, ChronoUnit.MINUTES).toString()),
                    ),
            ),
        )
}

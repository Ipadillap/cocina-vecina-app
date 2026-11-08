package cl.udec.cocinavecina.data.network

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class SignUpRequest(
    val email: String,
    val displayName: String,
    val password: String,
    val phone: String?,
    val role: String,
    val adult: Boolean,
    val acceptsTerms: Boolean,
)

@Serializable
data class SignUpResponse(val userId: Long, val message: String)

@Serializable
data class TokenResponse(
    val accessToken: String,
    val expiresInSeconds: Long,
    val userId: Long,
    val role: String,
    val verified: Boolean,
)

@Serializable
data class NearbyOffer(
    val id: Long,
    val title: String,
    val category: String,
    val photoUrl: String? = null,
    val basePrice: Int,
    val minPrice: Int,
    val availablePortions: Int,
    val expiresAt: String,
    val approxLatitude: Double,
    val approxLongitude: Double,
    val distanceMeters: Long,
)

@Serializable
data class OfferView(
    val id: Long,
    val sellerId: Long,
    val seller: String,
    val title: String,
    val description: String? = null,
    val category: String,
    val photoUrl: String? = null,
    val basePrice: Int,
    val minPrice: Int,
    val availablePortions: Int,
    val allergens: List<String>,
    val preparedAt: String,
    val expiresAt: String,
    val status: String,
    val approxLatitude: Double,
    val approxLongitude: Double,
)

@Serializable
data class NewPickupPoint(
    val alias: String,
    val address: String,
    val instructions: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

@Serializable
data class PickupPointView(val id: Long, val alias: String, val approxLatitude: Double, val approxLongitude: Double)

@Serializable
data class NewOffer(
    val pickupPointId: Long,
    val title: String,
    val description: String?,
    val category: String,
    val photoKey: String?,
    val basePrice: Int,
    val minPrice: Int,
    val portions: Int,
    val allergens: List<String>,
    val excludedIngredients: List<String>,
    val preparedAt: String,
    val expiresAt: String,
)

@Serializable
data class UploadRequest(val contentType: String)

@Serializable
data class UploadUrl(val key: String, val url: String, val method: String, val contentType: String)

@Serializable
data class NewPurchaseRequest(val portions: Int, val unitPrice: Int? = null)

@Serializable
data class Price(val unitPrice: Int)

@Serializable
data class ProposalView(val party: String, val unitPrice: Int, val createdAt: String)

@Serializable
data class PurchaseRequestView(
    val id: Long,
    val offerId: Long,
    val portions: Int,
    val rounds: Int,
    val currentPrice: Int,
    val currentParty: String,
    val turn: String? = null,
    val status: String,
    val closeReason: String? = null,
    val orderId: Long? = null,
    val proposals: List<ProposalView> = emptyList(),
)

@Serializable
data class OrderView(
    val id: Long,
    val offerId: Long,
    val buyerId: Long,
    val sellerId: Long,
    val agreedPrice: Int,
    val portions: Int,
    val status: String,
    val acceptedAt: String,
    val readyAt: String? = null,
    val deliveredAt: String? = null,
    val windowEnd: String,
)

@Serializable
data class CodeView(val orderId: Long, val code: String, val validUntil: String)

@Serializable
data class CodeValidation(val code: String)

@Serializable
data class Cancellation(val reason: String)

@Serializable
data class PickupDetailsView(
    val address: String,
    val instructions: String? = null,
    val latitude: Double,
    val longitude: Double,
    val phone: String? = null,
)

@Serializable
data class NewReport(val reason: String, val detail: String? = null)

@Serializable
data class ReportView(val reason: String, val detail: String? = null, val createdAt: String)

@Serializable
data class OfferUnderReview(
    val offerId: Long,
    val title: String,
    val sellerId: Long,
    val sellerReportCount: Long,
    val reports: List<ReportView>,
)

@Serializable
data class Decision(val action: String, val reason: String)

@Serializable
data class DeviceRegistration(val pushToken: String)

@Serializable
data class UserEvent(val type: String, val entity: String, val entityId: Long, val status: String)

@Serializable
data class Problem(val detail: String? = null, val rule: String? = null, val status: Int? = null)

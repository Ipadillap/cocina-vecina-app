package cl.udec.cocinavecina.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CocinaVecinaApi {
    @POST("v1/auth/login")
    suspend fun login(@Body s: LoginRequest): TokenResponse

    @POST("v1/auth/register")
    suspend fun register(@Body s: SignUpRequest): SignUpResponse

    @GET("v1/offers/nearby")
    suspend fun nearby(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("radius") radius: Int,
        @Query("order") order: String,
    ): List<NearbyOffer>

    @GET("v1/offers/{id}")
    suspend fun offer(@Path("id") id: Long): OfferView

    @GET("v1/pickup-points")
    suspend fun myPickupPoints(): List<PickupPointView>

    @POST("v1/pickup-points")
    suspend fun createPickupPoint(@Body p: NewPickupPoint): PickupPointView

    @POST("v1/photos/uploads")
    suspend fun prepareUpload(@Body s: UploadRequest): UploadUrl

    @POST("v1/offers")
    suspend fun publish(@Body o: NewOffer): OfferView

    @POST("v1/offers/{id}/purchase-requests")
    suspend fun submit(@Path("id") offerId: Long, @Body s: NewPurchaseRequest): PurchaseRequestView

    @GET("v1/purchase-requests/mine")
    suspend fun myRequests(): List<PurchaseRequestView>

    @GET("v1/purchase-requests/pending")
    suspend fun pendingRequests(): List<PurchaseRequestView>

    @POST("v1/purchase-requests/{id}/counteroffers")
    suspend fun counteroffer(@Path("id") id: Long, @Body p: Price): PurchaseRequestView

    @POST("v1/purchase-requests/{id}/proposals")
    suspend fun propose(@Path("id") id: Long, @Body p: Price): PurchaseRequestView

    @POST("v1/purchase-requests/{id}/accept")
    suspend fun accept(@Path("id") id: Long): PurchaseRequestView

    @POST("v1/purchase-requests/{id}/reject")
    suspend fun reject(@Path("id") id: Long): PurchaseRequestView

    @GET("v1/orders")
    suspend fun myOrders(): List<OrderView>

    @GET("v1/orders/{id}")
    suspend fun order(@Path("id") id: Long): OrderView

    @POST("v1/orders/{id}/code")
    suspend fun issueCode(@Path("id") id: Long): CodeView

    @PATCH("v1/orders/{id}/ready")
    suspend fun markReady(@Path("id") id: Long): OrderView

    @POST("v1/orders/{id}/delivery")
    suspend fun validateDelivery(@Path("id") id: Long, @Body v: CodeValidation): OrderView

    @POST("v1/orders/{id}/cancellation")
    suspend fun cancel(@Path("id") id: Long, @Body c: Cancellation): OrderView

    @GET("v1/orders/{id}/pickup")
    suspend fun pickupDetails(@Path("id") id: Long): PickupDetailsView

    @POST("v1/offers/{id}/reports")
    suspend fun report(@Path("id") offerId: Long, @Body r: NewReport): Response<Unit>

    @GET("v1/moderation/hidden-offers")
    suspend fun moderationQueue(): List<OfferUnderReview>

    @PATCH("v1/moderation/offers/{id}")
    suspend fun decideOffer(@Path("id") offerId: Long, @Body d: Decision): Response<Unit>

    @POST("v1/devices")
    suspend fun registerDevice(@Body r: DeviceRegistration): Response<Unit>
}

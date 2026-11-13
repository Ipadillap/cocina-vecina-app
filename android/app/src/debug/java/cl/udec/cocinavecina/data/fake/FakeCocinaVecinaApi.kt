package cl.udec.cocinavecina.data.fake

import cl.udec.cocinavecina.data.network.Cancellation
import cl.udec.cocinavecina.data.network.CocinaVecinaApi
import cl.udec.cocinavecina.data.network.CodeValidation
import cl.udec.cocinavecina.data.network.CodeView
import cl.udec.cocinavecina.data.network.Decision
import cl.udec.cocinavecina.data.network.DeviceRegistration
import cl.udec.cocinavecina.data.network.LoginRequest
import cl.udec.cocinavecina.data.network.NearbyOffer
import cl.udec.cocinavecina.data.network.NewOffer
import cl.udec.cocinavecina.data.network.NewPickupPoint
import cl.udec.cocinavecina.data.network.NewPurchaseRequest
import cl.udec.cocinavecina.data.network.NewReport
import cl.udec.cocinavecina.data.network.OfferUnderReview
import cl.udec.cocinavecina.data.network.OfferView
import cl.udec.cocinavecina.data.network.OrderView
import cl.udec.cocinavecina.data.network.PickupDetailsView
import cl.udec.cocinavecina.data.network.PickupPointView
import cl.udec.cocinavecina.data.network.Price
import cl.udec.cocinavecina.data.network.Problem
import cl.udec.cocinavecina.data.network.ProposalView
import cl.udec.cocinavecina.data.network.PurchaseRequestView
import cl.udec.cocinavecina.data.network.SignUpRequest
import cl.udec.cocinavecina.data.network.SignUpResponse
import cl.udec.cocinavecina.data.network.TokenResponse
import cl.udec.cocinavecina.data.network.UploadRequest
import cl.udec.cocinavecina.data.network.UploadUrl
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class FakeCocinaVecinaApi(
    private val currentToken: () -> String?,
    private val latencyMillis: Long = 400,
) : CocinaVecinaApi {
    private data class FakeRequest(val buyerId: Long, val view: PurchaseRequestView)

    private data class FakeOrder(val view: OrderView, val code: String? = null)

    private val mutex = Mutex()
    private val users = FakeSeed.users().toMutableList()
    private val pickupPoints = FakeSeed.pickupPoints().toMutableList()
    private val offers = FakeSeed.offers(Instant.now()).toMutableList()
    private val hiddenOffers = FakeSeed.hiddenOffers(Instant.now()).toMutableList()
    private val requests = mutableListOf<FakeRequest>()
    private val orders = mutableListOf<FakeOrder>()

    init {
        val now = Instant.now()
        requests +=
            FakeRequest(
                buyerId = FakeSeed.BUYER_ID,
                view =
                    PurchaseRequestView(
                        id = 1L,
                        offerId = 1L,
                        portions = 1,
                        rounds = 1,
                        currentPrice = 4000,
                        currentParty = BUYER,
                        turn = SELLER,
                        status = "OFFER_SENT",
                        proposals = listOf(ProposalView(BUYER, 4000, now.minus(15, ChronoUnit.MINUTES).toString())),
                    ),
            )
        orders +=
            FakeOrder(
                OrderView(
                    id = 1L,
                    offerId = 2L,
                    buyerId = FakeSeed.BUYER_ID,
                    sellerId = FakeSeed.SELLER_ID,
                    agreedPrice = 3200,
                    portions = 2,
                    status = "ACCEPTED",
                    acceptedAt = now.minus(20, ChronoUnit.MINUTES).toString(),
                    windowEnd = now.plus(2, ChronoUnit.HOURS).toString(),
                ),
            )
    }

    override suspend fun login(s: LoginRequest): TokenResponse =
        respond {
            val user = users.firstOrNull { it.email.equals(s.email.trim(), ignoreCase = true) && it.password == s.password }
            when {
                user == null -> fail(401, "Correo o contraseña incorrectos.")
                !user.verified -> fail(403, "Tu cuenta aún no está verificada.")
                else -> TokenResponse("$TOKEN_PREFIX${user.id}", 900, user.id, user.role, user.verified)
            }
        }

    override suspend fun register(s: SignUpRequest): SignUpResponse =
        respond {
            if (!s.adult || !s.acceptsTerms) fail(422, "Debes ser mayor de edad y aceptar los términos.")
            if (users.any { it.email.equals(s.email.trim(), ignoreCase = true) }) fail(409, "Ya existe una cuenta con ese correo.")
            if (s.role !in listOf(BUYER, SELLER)) fail(400, "Rol no permitido.")
            val user =
                FakeUser(
                    id = nextId(users.map { it.id }),
                    email = s.email.trim(),
                    displayName = s.displayName.trim(),
                    password = s.password,
                    role = s.role,
                    verified = true,
                    phone = s.phone,
                )
            users += user
            SignUpResponse(user.id, "Cuenta de prueba creada. En la API de prueba el correo queda verificado de inmediato.")
        }

    override suspend fun nearby(
        lat: Double,
        lon: Double,
        radius: Int,
        order: String,
    ): List<NearbyOffer> =
        respond {
            val now = Instant.now()
            val found =
                offers
                    .filter { it.status == ACTIVE && it.availablePortions > 0 && it.expiresAt.isAfter(now) }
                    .map { offer ->
                        val point = pointOf(offer)
                        val approxLatitude = approximate(point.latitude)
                        val approxLongitude = approximate(point.longitude)
                        NearbyOffer(
                            id = offer.id,
                            title = offer.title,
                            category = offer.category,
                            basePrice = offer.basePrice,
                            minPrice = offer.minPrice,
                            availablePortions = offer.availablePortions,
                            expiresAt = offer.expiresAt.toString(),
                            approxLatitude = approxLatitude,
                            approxLongitude = approxLongitude,
                            distanceMeters = distanceMeters(lat, lon, approxLatitude, approxLongitude),
                        )
                    }
                    .filter { it.distanceMeters <= radius }
            when (order) {
                "PRICE" -> found.sortedBy { it.basePrice }
                "EXPIRATION", "EXPIRES_AT" -> found.sortedBy { it.expiresAt }
                else -> found.sortedBy { it.distanceMeters }
            }
        }

    override suspend fun offer(id: Long): OfferView = respond { viewOf(findOffer(id)) }

    override suspend fun myPickupPoints(): List<PickupPointView> =
        respond {
            val user = requireUser()
            pickupPoints.filter { it.sellerId == user.id }.map { it.toView() }
        }

    override suspend fun createPickupPoint(p: NewPickupPoint): PickupPointView =
        respond {
            val user = requireUser()
            val point =
                FakePickupPoint(
                    id = nextId(pickupPoints.map { it.id }),
                    sellerId = user.id,
                    alias = p.alias,
                    address = p.address,
                    instructions = p.instructions,
                    latitude = p.latitude ?: CENTER_LATITUDE,
                    longitude = p.longitude ?: CENTER_LONGITUDE,
                )
            pickupPoints += point
            point.toView()
        }

    override suspend fun prepareUpload(s: UploadRequest): UploadUrl = respond { fail(501, "La subida de fotos no está disponible en la API de prueba.") }

    override suspend fun publish(o: NewOffer): OfferView =
        respond {
            val user = requireUser()
            if (user.role != SELLER) fail(403, "Solo las cuentas vendedoras pueden publicar.")
            if (pickupPoints.none { it.id == o.pickupPointId && it.sellerId == user.id }) fail(422, "El punto de retiro no es tuyo.")
            if (o.minPrice > o.basePrice) fail(422, "El precio mínimo no puede superar el precio base.")
            val offer =
                FakeOffer(
                    id = nextId(offers.map { it.id }),
                    sellerId = user.id,
                    pickupPointId = o.pickupPointId,
                    title = o.title,
                    description = o.description,
                    category = o.category,
                    basePrice = o.basePrice,
                    minPrice = o.minPrice,
                    availablePortions = o.portions,
                    allergens = o.allergens,
                    preparedAt = Instant.parse(o.preparedAt),
                    expiresAt = Instant.parse(o.expiresAt),
                    status = ACTIVE,
                )
            offers += offer
            viewOf(offer)
        }

    override suspend fun submit(
        offerId: Long,
        s: NewPurchaseRequest,
    ): PurchaseRequestView =
        respond {
            val user = requireUser()
            val offer = findOffer(offerId)
            if (offer.sellerId == user.id) fail(422, "No puedes comprar tu propia oferta.")
            if (offer.status != ACTIVE) fail(422, "La oferta ya no está disponible.")
            if (s.portions !in 1..offer.availablePortions) fail(422, "No quedan suficientes porciones.")
            val price = s.unitPrice ?: offer.basePrice
            if (price < offer.minPrice) fail(422, "El precio propuesto está bajo el mínimo aceptado.")
            val view =
                PurchaseRequestView(
                    id = nextId(requests.map { it.view.id }),
                    offerId = offerId,
                    portions = s.portions,
                    rounds = 1,
                    currentPrice = price,
                    currentParty = BUYER,
                    turn = SELLER,
                    status = "OFFER_SENT",
                    proposals = listOf(ProposalView(BUYER, price, Instant.now().toString())),
                )
            requests += FakeRequest(user.id, view)
            view
        }

    override suspend fun myRequests(): List<PurchaseRequestView> =
        respond {
            val user = requireUser()
            requests.filter { it.buyerId == user.id }.map { it.view }
        }

    override suspend fun pendingRequests(): List<PurchaseRequestView> =
        respond {
            val user = requireUser()
            requests.filter { findOffer(it.view.offerId).sellerId == user.id && it.view.turn != null }.map { it.view }
        }

    override suspend fun counteroffer(
        id: Long,
        p: Price,
    ): PurchaseRequestView = respond { negotiate(id, SELLER, p.unitPrice, "COUNTEROFFER") }

    override suspend fun propose(
        id: Long,
        p: Price,
    ): PurchaseRequestView = respond { negotiate(id, BUYER, p.unitPrice, "OFFER_SENT") }

    override suspend fun accept(id: Long): PurchaseRequestView =
        respond {
            val request = findRequest(id)
            requireTurn(request)
            val offer = findOffer(request.view.offerId)
            if (request.view.portions > offer.availablePortions) fail(422, "No quedan suficientes porciones.")
            replaceOffer(offer.copy(availablePortions = offer.availablePortions - request.view.portions))
            val now = Instant.now()
            val order =
                OrderView(
                    id = nextId(orders.map { it.view.id }),
                    offerId = offer.id,
                    buyerId = request.buyerId,
                    sellerId = offer.sellerId,
                    agreedPrice = request.view.currentPrice,
                    portions = request.view.portions,
                    status = "ACCEPTED",
                    acceptedAt = now.toString(),
                    windowEnd = now.plus(2, ChronoUnit.HOURS).toString(),
                )
            orders += FakeOrder(order)
            updateRequest(request) { it.copy(status = "ACCEPTED", turn = null, orderId = order.id) }
        }

    override suspend fun reject(id: Long): PurchaseRequestView =
        respond {
            val request = findRequest(id)
            requireTurn(request)
            updateRequest(request) { it.copy(status = "REJECTED", turn = null, closeReason = "REJECTED") }
        }

    override suspend fun myOrders(): List<OrderView> =
        respond {
            val user = requireUser()
            orders.map { it.view }.filter { it.buyerId == user.id || it.sellerId == user.id }
        }

    override suspend fun order(id: Long): OrderView = respond { findOrder(id).view }

    override suspend fun issueCode(id: Long): CodeView =
        respond {
            val order = findOrder(id)
            if (requireUser().id != order.view.buyerId) fail(403, "Solo quien compra puede generar el código.")
            val code = order.code ?: Random.nextInt(100_000, 1_000_000).toString()
            replaceOrder(order.copy(code = code))
            CodeView(id, code, order.view.windowEnd)
        }

    override suspend fun markReady(id: Long): OrderView =
        respond {
            val order = findOrder(id)
            if (requireUser().id != order.view.sellerId) fail(403, "Solo quien vende puede marcar el pedido como listo.")
            if (order.view.status != "ACCEPTED") fail(422, "El pedido no se puede marcar como listo.")
            replaceOrder(order.copy(view = order.view.copy(status = "READY_FOR_PICKUP", readyAt = Instant.now().toString()))).view
        }

    override suspend fun validateDelivery(
        id: Long,
        v: CodeValidation,
    ): OrderView =
        respond {
            val order = findOrder(id)
            if (requireUser().id != order.view.sellerId) fail(403, "Solo quien vende puede validar la entrega.")
            if (order.code == null || order.code != v.code.trim()) fail(422, "El código no coincide.")
            replaceOrder(order.copy(view = order.view.copy(status = "DELIVERED", deliveredAt = Instant.now().toString()))).view
        }

    override suspend fun cancel(
        id: Long,
        c: Cancellation,
    ): OrderView =
        respond {
            val order = findOrder(id)
            if (order.view.status != "ACCEPTED") fail(422, "El pedido ya no se puede cancelar.")
            replaceOrder(order.copy(view = order.view.copy(status = "CANCELLED"))).view
        }

    override suspend fun pickupDetails(id: Long): PickupDetailsView =
        respond {
            val order = findOrder(id)
            val point = pointOf(findOffer(order.view.offerId))
            val seller = users.first { it.id == order.view.sellerId }
            PickupDetailsView(point.address, point.instructions, point.latitude, point.longitude, seller.phone)
        }

    override suspend fun report(
        offerId: Long,
        r: NewReport,
    ): Response<Unit> =
        respond {
            findOffer(offerId)
            Response.success(Unit)
        }

    override suspend fun moderationQueue(): List<OfferUnderReview> =
        respond {
            if (requireUser().role != MODERATOR) fail(403, "Solo moderación puede ver esta cola.")
            hiddenOffers.toList()
        }

    override suspend fun decideOffer(
        offerId: Long,
        d: Decision,
    ): Response<Unit> =
        respond {
            if (requireUser().role != MODERATOR) fail(403, "Solo moderación puede decidir.")
            val offer = findOffer(offerId)
            replaceOffer(offer.copy(status = if (d.action == "REACTIVATE") ACTIVE else "WITHDRAWN"))
            hiddenOffers.removeAll { it.offerId == offerId }
            Response.success(Unit)
        }

    override suspend fun registerDevice(r: DeviceRegistration): Response<Unit> = respond { Response.success(Unit) }

    private suspend fun <T> respond(block: () -> T): T {
        delay(latencyMillis)
        return mutex.withLock { block() }
    }

    private fun requireUser(): FakeUser {
        val id = currentToken()?.removePrefix(TOKEN_PREFIX)?.toLongOrNull() ?: fail(401, "Inicia sesión para continuar.")
        return users.firstOrNull { it.id == id } ?: fail(401, "Tu sesión de prueba expiró. Vuelve a iniciar sesión.")
    }

    private fun requireTurn(request: FakeRequest) {
        val user = requireUser()
        val party = if (user.id == request.buyerId) BUYER else SELLER
        if (request.view.turn != party) fail(422, "No es tu turno en esta negociación.")
    }

    private fun negotiate(
        id: Long,
        party: String,
        price: Int,
        status: String,
    ): PurchaseRequestView {
        val request = findRequest(id)
        requireTurn(request)
        val offer = findOffer(request.view.offerId)
        if (price < offer.minPrice || price > offer.basePrice) fail(422, "El precio debe estar entre el mínimo y el precio base.")
        if (request.view.rounds >= MAX_ROUNDS) fail(422, "Se alcanzó el máximo de rondas de negociación.")
        return updateRequest(request) {
            it.copy(
                rounds = it.rounds + 1,
                currentPrice = price,
                currentParty = party,
                turn = if (party == BUYER) SELLER else BUYER,
                status = status,
                proposals = it.proposals + ProposalView(party, price, Instant.now().toString()),
            )
        }
    }

    private fun findOffer(id: Long): FakeOffer = offers.firstOrNull { it.id == id } ?: fail(404, "La oferta no existe.")

    private fun findRequest(id: Long): FakeRequest = requests.firstOrNull { it.view.id == id } ?: fail(404, "La solicitud no existe.")

    private fun findOrder(id: Long): FakeOrder {
        val user = requireUser()
        val order = orders.firstOrNull { it.view.id == id } ?: fail(404, "El pedido no existe.")
        if (user.id != order.view.buyerId && user.id != order.view.sellerId) fail(403, "Este pedido no es tuyo.")
        return order
    }

    private fun pointOf(offer: FakeOffer): FakePickupPoint = pickupPoints.first { it.id == offer.pickupPointId }

    private fun replaceOffer(offer: FakeOffer) {
        offers[offers.indexOfFirst { it.id == offer.id }] = offer
    }

    private fun replaceOrder(order: FakeOrder): FakeOrder {
        orders[orders.indexOfFirst { it.view.id == order.view.id }] = order
        return order
    }

    private fun updateRequest(
        request: FakeRequest,
        change: (PurchaseRequestView) -> PurchaseRequestView,
    ): PurchaseRequestView {
        val updated = request.copy(view = change(request.view))
        requests[requests.indexOf(request)] = updated
        return updated.view
    }

    private fun viewOf(offer: FakeOffer): OfferView {
        val point = pointOf(offer)
        return OfferView(
            id = offer.id,
            sellerId = offer.sellerId,
            seller = users.first { it.id == offer.sellerId }.displayName,
            title = offer.title,
            description = offer.description,
            category = offer.category,
            basePrice = offer.basePrice,
            minPrice = offer.minPrice,
            availablePortions = offer.availablePortions,
            allergens = offer.allergens.ifEmpty { listOf("NONE_KNOWN") },
            preparedAt = offer.preparedAt.toString(),
            expiresAt = offer.expiresAt.toString(),
            status = offer.status,
            approxLatitude = approximate(point.latitude),
            approxLongitude = approximate(point.longitude),
        )
    }

    private fun FakePickupPoint.toView() = PickupPointView(id, alias, approximate(latitude), approximate(longitude))

    private fun nextId(ids: List<Long>): Long = (ids.maxOrNull() ?: 0L) + 1

    private fun approximate(value: Double): Double = Math.round(value / ZONE_DEGREES) * ZONE_DEGREES

    private fun distanceMeters(
        fromLatitude: Double,
        fromLongitude: Double,
        toLatitude: Double,
        toLongitude: Double,
    ): Long {
        val deltaLatitude = Math.toRadians(toLatitude - fromLatitude)
        val deltaLongitude = Math.toRadians(toLongitude - fromLongitude)
        val a =
            sin(deltaLatitude / 2) * sin(deltaLatitude / 2) +
                cos(Math.toRadians(fromLatitude)) * cos(Math.toRadians(toLatitude)) *
                sin(deltaLongitude / 2) * sin(deltaLongitude / 2)
        return (EARTH_RADIUS_METERS * 2 * atan2(sqrt(a), sqrt(1 - a))).roundToLong()
    }

    private fun fail(
        code: Int,
        detail: String,
    ): Nothing {
        val body = Json.encodeToString(Problem.serializer(), Problem(detail = detail, status = code))
        throw HttpException(Response.error<Any>(code, body.toResponseBody(PROBLEM_JSON)))
    }

    private companion object {
        const val TOKEN_PREFIX = "fake-token-"
        const val BUYER = "BUYER"
        const val SELLER = "SELLER"
        const val MODERATOR = "MODERATOR"
        const val ACTIVE = "ACTIVE"
        const val MAX_ROUNDS = 3
        const val CENTER_LATITUDE = -36.8270
        const val CENTER_LONGITUDE = -73.0503
        const val ZONE_DEGREES = 0.0036
        const val EARTH_RADIUS_METERS = 6_371_000.0
        val PROBLEM_JSON = "application/problem+json".toMediaType()
    }
}

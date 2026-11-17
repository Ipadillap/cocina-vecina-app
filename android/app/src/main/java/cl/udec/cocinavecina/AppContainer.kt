package cl.udec.cocinavecina

import android.content.Context
import cl.udec.cocinavecina.data.ApiProvider
import cl.udec.cocinavecina.data.Repository
import cl.udec.cocinavecina.data.network.CocinaVecinaApi
import cl.udec.cocinavecina.data.network.DeviceRegistration
import cl.udec.cocinavecina.data.network.RealtimeEvents
import cl.udec.cocinavecina.data.network.RefreshRequest
import cl.udec.cocinavecina.data.network.TokenAuthenticator
import cl.udec.cocinavecina.data.session.SessionStore
import cl.udec.cocinavecina.notifications.PushToken
import cl.udec.cocinavecina.ui.verification.EmailVerification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

    val session = SessionStore(context)

    private val baseClient =
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
                }
            }
            .build()

    private val authClient =
        baseClient.newBuilder()
            .addInterceptor { chain ->
                val token = session.currentToken
                val request = chain.request()
                // Un token vencido en /v1/auth/* haría que la API rechace el inicio de sesión con 401.
                val isAuthCall = request.url.encodedPath.contains("/v1/auth/")
                chain.proceed(
                    if (token == null || isAuthCall) request else request.newBuilder().header("Authorization", "Bearer $token").build(),
                )
            }
            .authenticator(TokenAuthenticator(session, baseClient, BuildConfig.API_URL + "/", json))
            .build()

    private val remoteApi: CocinaVecinaApi =
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_URL + "/")
            .client(authClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(CocinaVecinaApi::class.java)

    private val api: CocinaVecinaApi = ApiProvider.create(remoteApi, session)

    val repository = Repository(api, session, baseClient, authClient, json)

    val events = RealtimeEvents(authClient.newBuilder().pingInterval(30, TimeUnit.SECONDS).build(), session, json)

    val verification = EmailVerification(repository, appScope) { registerPush() }

    /** Registra el token de FCM del teléfono para la sesión actual (el servidor lo exige verificada). */
    fun registerPush() {
        appScope.launch {
            val token = PushToken.current(appContext) ?: return@launch
            repository.call { registerDevice(DeviceRegistration(token)) }
        }
    }

    /** Da de baja el teléfono y revoca el token de renovación en el servidor; si no hay red, se cierra igual. */
    suspend fun endSession() {
        withTimeoutOrNull(END_SESSION_TIMEOUT_MILLIS) {
            PushToken.current(appContext)?.let { token -> repository.call { unregisterDevice(token) } }
            // Después de la baja: si el token de acceso había vencido, ya se renovó y cambió el de renovación.
            session.currentRefreshToken?.let { refresh -> repository.call { logout(RefreshRequest(refresh)) } }
        }
    }

    private companion object {
        const val END_SESSION_TIMEOUT_MILLIS = 4_000L
    }
}

package cl.udec.cocinavecina

import android.content.Context
import cl.udec.cocinavecina.data.ApiProvider
import cl.udec.cocinavecina.data.Repository
import cl.udec.cocinavecina.data.network.CocinaVecinaApi
import cl.udec.cocinavecina.data.network.RealtimeEvents
import cl.udec.cocinavecina.data.network.TokenAuthenticator
import cl.udec.cocinavecina.data.session.SessionStore
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {
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
}

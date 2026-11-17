package cl.udec.cocinavecina.data.network

import cl.udec.cocinavecina.data.session.SessionStore
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import java.io.IOException

/**
 * Renueva el token de acceso (15 min) con el token de renovación cuando la API responde 401.
 * Si la renovación es rechazada, cierra la sesión; si falla por red, deja pasar el error de conexión.
 */
class TokenAuthenticator(
    private val session: SessionStore,
    private val plainClient: OkHttpClient,
    private val baseUrl: String,
    private val json: Json,
) : Authenticator {
    private val lock = Any()

    override fun authenticate(
        route: Route?,
        response: Response,
    ): Request? {
        val sent = response.request.header("Authorization")?.removePrefix("Bearer ") ?: return null
        if (response.request.url.encodedPath.contains(AUTH_PATH)) return null
        if (response.priorResponse != null) return null
        return synchronized(lock) { renew(response.request, sent) }
    }

    private fun renew(
        failed: Request,
        sent: String,
    ): Request? {
        val current = session.currentToken ?: return null
        if (current != sent) return withToken(failed, current)
        val refresh = session.currentRefreshToken
        if (refresh == null) {
            runBlocking { session.clear() }
            return null
        }
        val body = json.encodeToString(RefreshRequest.serializer(), RefreshRequest(refresh))
        val request =
            Request.Builder()
                .url(baseUrl + "v1/auth/refresh")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()
        plainClient.newCall(request).execute().use { r ->
            if (r.isSuccessful) {
                val tokens = json.decodeFromString(TokenResponse.serializer(), r.body!!.string())
                runBlocking { session.save(tokens) }
                return withToken(failed, tokens.accessToken)
            }
            if (r.code == 401 || r.code == 403) {
                runBlocking { session.clear() }
                return null
            }
            throw IOException("No se pudo renovar la sesión (${r.code})")
        }
    }

    private fun withToken(
        request: Request,
        token: String,
    ): Request = request.newBuilder().header("Authorization", "Bearer $token").build()

    private companion object {
        const val AUTH_PATH = "/v1/auth/"
    }
}

package cl.udec.cocinavecina.data

import cl.udec.cocinavecina.BuildConfig
import cl.udec.cocinavecina.data.network.CocinaVecinaApi
import cl.udec.cocinavecina.data.network.Problem
import cl.udec.cocinavecina.data.network.UploadRequest
import cl.udec.cocinavecina.data.session.SessionStore
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException

class Repository(
    val api: CocinaVecinaApi,
    val session: SessionStore,
    private val plainClient: OkHttpClient,
    private val authClient: OkHttpClient,
    private val json: Json,
) {
    suspend fun <T> call(block: suspend CocinaVecinaApi.() -> T): Result<T> = try {
        Result.success(api.block())
    } catch (e: HttpException) {
        if (e.code() == 401) session.clear()
        val body = e.response()?.errorBody()?.string()
        val detail = body?.let { runCatching { json.decodeFromString(Problem.serializer(), it).detail }.getOrNull() }
        Result.failure(ApiError(e.code(), detail ?: "Error ${e.code()}"))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(ApiError(0, "Sin conexión con el servidor"))
    }

    fun absoluteUrl(url: String?): String? = url?.let { if (it.startsWith("/")) BuildConfig.API_URL + it else it }

    suspend fun uploadPhoto(jpeg: ByteArray): Result<String> = call { prepareUpload(UploadRequest("image/jpeg")) }
        .mapCatching { destination ->
            withContext(Dispatchers.IO) {
                val url = absoluteUrl(destination.url)!!
                val client = if (url.startsWith(BuildConfig.API_URL)) authClient else plainClient
                val put = Request.Builder()
                    .url(url)
                    .put(jpeg.toRequestBody(destination.contentType.toMediaType()))
                    .build()
                client.newCall(put).execute().use { r ->
                    if (!r.isSuccessful) throw ApiError(r.code, "No se pudo subir la foto")
                }
            }
            destination.key
        }
}

class ApiError(val code: Int, override val message: String) : Exception(message)

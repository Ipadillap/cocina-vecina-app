package cl.udec.cocinavecina.data.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import cl.udec.cocinavecina.data.network.TokenResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore by preferencesDataStore(name = "session")

sealed interface SessionState {
    data object Loading : SessionState

    data object Anonymous : SessionState

    data class Active(val userId: Long, val role: String) : SessionState {
        val isSeller get() = role == "SELLER"
        val isModerator get() = role == "MODERATOR"
    }
}

class SessionStore(private val context: Context) {
    private object Keys {
        val TOKEN = stringPreferencesKey("token")
        val USER = longPreferencesKey("user")
        val ROLE = stringPreferencesKey("role")
        val REFRESH = stringPreferencesKey("refresh")

        fun code(orderId: Long) = stringPreferencesKey("code_$orderId")
    }

    @Volatile
    var currentToken: String? = null
        private set

    @Volatile
    var currentRefreshToken: String? = null
        private set

    val sessionState: Flow<SessionState> =
        context.sessionDataStore.data.map { p ->
            val token = p[Keys.TOKEN]
            currentToken = token
            currentRefreshToken = p[Keys.REFRESH]
            if (token == null) {
                SessionState.Anonymous
            } else {
                SessionState.Active(p[Keys.USER] ?: 0L, p[Keys.ROLE] ?: "BUYER")
            }
        }

    suspend fun load() {
        val p = context.sessionDataStore.data.first()
        currentToken = p[Keys.TOKEN]
        currentRefreshToken = p[Keys.REFRESH]
    }

    suspend fun save(t: TokenResponse) {
        context.sessionDataStore.edit {
            it[Keys.TOKEN] = t.accessToken
            it[Keys.USER] = t.userId
            it[Keys.ROLE] = t.role
            val refresh = t.refreshToken
            if (refresh != null) {
                it[Keys.REFRESH] = refresh
            } else {
                it.remove(Keys.REFRESH)
            }
        }
        currentToken = t.accessToken
        currentRefreshToken = t.refreshToken
    }

    suspend fun clear() {
        // Solo se borra la sesión; los códigos de entrega guardados se conservan.
        context.sessionDataStore.edit {
            it.remove(Keys.TOKEN)
            it.remove(Keys.USER)
            it.remove(Keys.ROLE)
            it.remove(Keys.REFRESH)
        }
        currentToken = null
        currentRefreshToken = null
    }

    suspend fun saveCode(
        orderId: Long,
        code: String,
    ) {
        context.sessionDataStore.edit { it[Keys.code(orderId)] = code }
    }

    suspend fun code(orderId: Long): String? = context.sessionDataStore.data.first()[Keys.code(orderId)]
}

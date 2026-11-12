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

        fun code(orderId: Long) = stringPreferencesKey("code_$orderId")
    }

    @Volatile
    var currentToken: String? = null
        private set

    val sessionState: Flow<SessionState> =
        context.sessionDataStore.data.map { p ->
            val token = p[Keys.TOKEN]
            currentToken = token
            if (token == null) {
                SessionState.Anonymous
            } else {
                SessionState.Active(p[Keys.USER] ?: 0L, p[Keys.ROLE] ?: "BUYER")
            }
        }

    suspend fun load() {
        currentToken = context.sessionDataStore.data.first()[Keys.TOKEN]
    }

    suspend fun save(t: TokenResponse) {
        context.sessionDataStore.edit {
            it[Keys.TOKEN] = t.accessToken
            it[Keys.USER] = t.userId
            it[Keys.ROLE] = t.role
        }
        currentToken = t.accessToken
    }

    suspend fun clear() {
        context.sessionDataStore.edit { it.clear() }
        currentToken = null
    }

    suspend fun saveCode(
        orderId: Long,
        code: String,
    ) {
        context.sessionDataStore.edit { it[Keys.code(orderId)] = code }
    }

    suspend fun code(orderId: Long): String? = context.sessionDataStore.data.first()[Keys.code(orderId)]
}

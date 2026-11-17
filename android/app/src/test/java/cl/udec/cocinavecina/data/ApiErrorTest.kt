package cl.udec.cocinavecina.data

import cl.udec.cocinavecina.data.network.Problem
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ApiErrorTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun problem(body: String) = json.decodeFromString(Problem.serializer(), body)

    @Test
    fun backendDetailListBecomesMessageAndFieldErrors() {
        val body =
            """{"title":"Datos inválidos","status":422,"detail":["phone: El teléfono es obligatorio para vender.","Revisa la fecha."]}"""
        val error = ApiError.from(422, problem(body))
        assertEquals("El teléfono es obligatorio para vender. Revisa la fecha.", error.message)
        assertEquals("El teléfono es obligatorio para vender.", error.fieldError("phone"))
        assertNull(error.fieldError("email"))
        assertEquals(listOf("Revisa la fecha."), error.otherErrors("phone"))
    }

    @Test
    fun stringDetailIsAccepted() {
        val error = ApiError.from(401, problem("""{"detail":"Correo o contraseña incorrectos.","status":401}"""))
        assertEquals("Correo o contraseña incorrectos.", error.message)
    }

    @Test
    fun emptyOrMissingDetailFallsBackToTitleOrCode() {
        assertEquals("Conflicto", ApiError.from(409, problem("""{"title":"Conflicto","status":409,"detail":[]}""")).message)
        assertEquals("Error 404", ApiError.from(404, problem("""{"timestamp":"x","status":404,"detail":null}""")).message)
        assertEquals("Error 500", ApiError.from(500, null).message)
    }
}

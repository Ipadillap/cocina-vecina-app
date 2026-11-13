package cl.udec.cocinavecina.data

import cl.udec.cocinavecina.data.network.CocinaVecinaApi
import cl.udec.cocinavecina.data.session.SessionStore

object ApiProvider {
    fun create(
        remoteApi: CocinaVecinaApi,
        session: SessionStore,
    ): CocinaVecinaApi = remoteApi
}

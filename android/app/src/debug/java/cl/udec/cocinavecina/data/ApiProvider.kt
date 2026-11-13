package cl.udec.cocinavecina.data

import cl.udec.cocinavecina.BuildConfig
import cl.udec.cocinavecina.data.fake.FakeCocinaVecinaApi
import cl.udec.cocinavecina.data.network.CocinaVecinaApi
import cl.udec.cocinavecina.data.session.SessionStore

object ApiProvider {
    fun create(
        remoteApi: CocinaVecinaApi,
        session: SessionStore,
    ): CocinaVecinaApi = if (BuildConfig.USE_FAKE_API) FakeCocinaVecinaApi(currentToken = { session.currentToken }) else remoteApi
}

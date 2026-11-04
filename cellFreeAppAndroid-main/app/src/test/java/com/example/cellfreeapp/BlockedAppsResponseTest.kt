package com.example.cellfreeapp

import com.example.cellfreeapp.data.model.BlockedAppsResponse
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

class BlockedAppsResponseTest {

    @Test
    fun parsesActiveAppsResponseFromServer() {
        val json = """
            {
              "status": "success",
              "data": {
                "aplicaciones_activas": [
                  {
                    "id": 1,
                    "nombre_app": "AirConsole",
                    "tiempo_bloqueo": 1,
                    "estado": true,
                    "nombre_paquete": "com.airconsole.controller"
                  }
                ]
              }
            }
        """.trimIndent()

        val response = Gson().fromJson(json, BlockedAppsResponse::class.java)
        val app = response.data.aplicacionesBloqueadas?.single()

        assertEquals("com.airconsole.controller", app?.nombrePaquete)
        assertEquals(1, app?.tiempoBloqueo)
    }

    @Test
    fun continuesParsingBlockedAppsResponseKey() {
        val json = """
            {
              "status": "success",
              "data": {
                "aplicaciones_bloqueadas": [
                  {
                    "id": 1,
                    "nombre_app": "AirConsole",
                    "nombre_paquete": "com.airconsole.controller"
                  }
                ]
              }
            }
        """.trimIndent()

        val response = Gson().fromJson(json, BlockedAppsResponse::class.java)

        assertEquals(1, response.data.aplicacionesBloqueadas?.size)
    }
}

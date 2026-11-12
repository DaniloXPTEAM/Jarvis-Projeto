package com.gabi.assistant.skills

import org.junit.Assert.*
import org.junit.Test

class WeatherAgentTest {
    @Test fun unknownCityDoesNotUseFortalezaCoordinates() {
        assertNull(WeatherAgent().resolveCity("Lisboa"))
    }
    @Test fun knownCityKeepsItsOwnCoordinates() {
        val city = WeatherAgent().resolveCity("clima em São Paulo")!!
        assertEquals("São Paulo", city.third)
        assertEquals(-23.5505, city.first, 0.0001)
    }
    @Test fun unspecifiedCityUsesConfiguredDefault() {
        assertEquals("Fortaleza", WeatherAgent().resolveCity(null)!!.third)
    }
}

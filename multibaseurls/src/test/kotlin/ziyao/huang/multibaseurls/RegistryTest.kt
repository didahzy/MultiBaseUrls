package ziyao.huang.multibaseurls

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

private object Production : Host
private object Testing : Host

class RegistryTest {

    @Before
    fun setUp() {
        MultiBaseUrls.clear()
    }

    @After
    fun tearDown() {
        MultiBaseUrls.clear()
        MultiBaseUrls.onUnregistered = null
    }

    @Test
    fun `baseUrl extension registers and exposes the URL`() {
        Production.baseUrl("https://api.example.com/")

        assertEquals("https://api.example.com/", MultiBaseUrls.urlOf(Production::class))
    }

    @Test
    fun `trailing slash is appended when missing`() {
        Production.baseUrl("https://api.example.com")

        assertEquals("https://api.example.com/", MultiBaseUrls.urlOf(Production::class))
    }

    @Test
    fun `invalid URL throws IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            Production.baseUrl("not a url")
        }
    }

    @Test
    fun `later registration overrides the earlier one`() {
        Production.baseUrl("https://old.example.com/")

        Production.baseUrl("https://new.example.com/")

        assertEquals("https://new.example.com/", MultiBaseUrls.urlOf(Production::class))
    }

    @Test
    fun `urlOf returns null for an unregistered marker`() {
        assertNull(MultiBaseUrls.urlOf(Testing::class))
    }

    @Test
    fun `unregister removes the registration`() {
        Production.baseUrl("https://api.example.com/")

        MultiBaseUrls.unregister(Production::class)

        assertNull(MultiBaseUrls.urlOf(Production::class))
    }
}

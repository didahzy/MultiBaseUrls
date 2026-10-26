package ziyao.huang.multibaseurls

import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

class BuilderExtTest {

    @Before
    fun setUp() {
        MultiBaseUrls.originalBaseUrl = null
    }

    @After
    fun tearDown() {
        MultiBaseUrls.originalBaseUrl = null
    }

    @Test
    fun `OkHttp Builder extension installs the MultiBaseUrls interceptor`() {
        val client = OkHttpClient.Builder().multiBaseUrls().build()

        assertTrue(client.interceptors.contains(MultiBaseUrls.interceptor))
    }

    @Test
    fun `Retrofit Builder extension builds and captures baseUrl automatically`() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://default.example.com/api/")
            .client(OkHttpClient())
            .multiBaseUrls()

        assertEquals("https://default.example.com/api/", MultiBaseUrls.originalBaseUrl?.toString())
        assertEquals("https://default.example.com/api/", retrofit.baseUrl().toString())
    }
}

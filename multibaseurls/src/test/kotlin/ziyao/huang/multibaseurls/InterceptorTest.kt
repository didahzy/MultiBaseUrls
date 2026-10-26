package ziyao.huang.multibaseurls

import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Url
import java.io.IOException

private object Prod : Host
private object TestEnv : Host

@BaseUrl(Prod::class)
interface ClassLevelApi {
    @GET("class-default")
    fun classDefault(): Call<Unit>

    @BaseUrl(TestEnv::class)
    @GET("override")
    fun overridden(): Call<Unit>

    @Ignore
    @GET("ignored")
    fun ignored(): Call<Unit>
}

interface MethodLevelApi {
    @BaseUrl(Prod::class)
    @GET("posts")
    fun posts(): Call<Unit>

    @BaseUrl(Prod::class)
    @GET("posts?limit=10&page=2")
    fun postsWithQuery(): Call<Unit>

    @GET("untagged")
    fun untagged(): Call<Unit>

    @GET
    fun dynamic(@Url url: String): Call<Unit>
}

interface PrefixedApi {
    @BaseUrl(Prod::class)
    @GET("posts")
    fun posts(): Call<Unit>

    @BaseUrl(Prod::class)
    @GET("nested/a/b")
    fun nested(): Call<Unit>

    @BaseUrl(Prod::class)
    @GET("posts?limit=10")
    fun withQuery(): Call<Unit>
}

class InterceptorTest {

    private var capturedUrl: String? = null

    private val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(MultiBaseUrls.interceptor)
        .addInterceptor { chain -> // records the final (post-switch) URL and aborts the real network call
            capturedUrl = chain.request().url.toString()
            throw IOException("captured")
        }
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://default.example.com/")
        .client(client)
        .build()

    /** A Retrofit whose original baseUrl carries a path prefix, for prefix-stripping tests. */
    private val prefixedRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://default.example.com/api/")
        .client(client)
        .build()

    @Before
    fun setUp() {
        MultiBaseUrls.clear()
        Prod.baseUrl("https://prod.example.com/")
        TestEnv.baseUrl("https://test.example.com/")
    }

    @After
    fun tearDown() {
        MultiBaseUrls.clear()
        MultiBaseUrls.onUnregistered = null
        MultiBaseUrls.originalBaseUrl = null
    }

    /** Fires the request; the capturing interceptor aborts it with an IOException. */
    private fun request(block: () -> Call<Unit>) {
        runCatching { block().execute() }
    }

    @Test
    fun `untagged method keeps the original baseUrl`() {
        val api = retrofit.create(MethodLevelApi::class.java)

        request { api.untagged() }

        assertEquals("https://default.example.com/untagged", capturedUrl)
    }

    @Test
    fun `method-level BaseUrl switches to the registered URL keeping the path`() {
        val api = retrofit.create(MethodLevelApi::class.java)

        request { api.posts() }

        assertEquals("https://prod.example.com/posts", capturedUrl)
    }

    @Test
    fun `query is preserved after switching`() {
        val api = retrofit.create(MethodLevelApi::class.java)

        request { api.postsWithQuery() }

        assertEquals("https://prod.example.com/posts?limit=10&page=2", capturedUrl)
    }

    @Test
    fun `class-level BaseUrl applies to all methods`() {
        val api = retrofit.create(ClassLevelApi::class.java)

        request { api.classDefault() }

        assertEquals("https://prod.example.com/class-default", capturedUrl)
    }

    @Test
    fun `method-level BaseUrl overrides class-level`() {
        val api = retrofit.create(ClassLevelApi::class.java)

        request { api.overridden() }

        assertEquals("https://test.example.com/override", capturedUrl)
    }

    @Test
    fun `Ignore method does not switch even with class-level BaseUrl`() {
        val api = retrofit.create(ClassLevelApi::class.java)

        request { api.ignored() }

        assertEquals("https://default.example.com/ignored", capturedUrl)
    }

    @Test
    fun `method with a Url parameter does not switch`() {
        val api = retrofit.create(MethodLevelApi::class.java)

        request { api.dynamic("https://anywhere.example.com/full") }

        assertEquals("https://anywhere.example.com/full", capturedUrl)
    }

    @Test
    fun `unregistered marker throws IllegalStateException by default`() {
        MultiBaseUrls.unregister(Prod::class)
        val api = retrofit.create(MethodLevelApi::class.java)

        assertThrows(IllegalStateException::class.java) { api.posts().execute() }
    }

    @Test
    fun `onUnregistered returning a URL switches to it`() {
        MultiBaseUrls.unregister(Prod::class)
        MultiBaseUrls.onUnregistered = { "https://fallback.example.com/" }
        val api = retrofit.create(MethodLevelApi::class.java)

        request { api.posts() }

        assertEquals("https://fallback.example.com/posts", capturedUrl)
    }

    @Test
    fun `onUnregistered returning null falls back to the original baseUrl`() {
        MultiBaseUrls.unregister(Prod::class)
        MultiBaseUrls.onUnregistered = { null }
        val api = retrofit.create(MethodLevelApi::class.java)

        request { api.posts() }

        assertEquals("https://default.example.com/posts", capturedUrl)
    }

    @Test
    fun `end-to-end request lands on the registered host and gets a response`() {
        mockwebserver3.MockWebServer().use { server ->
            server.start()
            server.enqueue(
                mockwebserver3.MockResponse.Builder().code(200).body("ok").build()
            )
            Prod.baseUrl(server.url("/").toString())
            val client = OkHttpClient.Builder()
                .addInterceptor(MultiBaseUrls.interceptor)
                .build()
            val retrofit = Retrofit.Builder()
                .baseUrl("https://default.example.com/")
                .client(client)
                .build()
            val api = retrofit.create(EndToEndApi::class.java)

            val response = api.posts().execute()

            assertEquals("ok", response.body()?.string())
            assertEquals("/posts", server.takeRequest().url.encodedPath)
        }
    }

    @Test
    fun `with originalBaseUrl set, the old prefix is stripped and the new prefix kept`() {
        MultiBaseUrls.setup {
            originalBaseUrl = "https://default.example.com/api/"
            Prod with "https://prod.example.com/v2/"
        }
        val api = prefixedRetrofit.create(PrefixedApi::class.java)

        request { api.posts() }

        assertEquals("https://prod.example.com/v2/posts", capturedUrl)
    }

    @Test
    fun `with originalBaseUrl set, nested relative paths join correctly`() {
        MultiBaseUrls.setup {
            originalBaseUrl = "https://default.example.com/api/"
            Prod with "https://prod.example.com/v2/"
        }
        val api = prefixedRetrofit.create(PrefixedApi::class.java)

        request { api.nested() }

        assertEquals("https://prod.example.com/v2/nested/a/b", capturedUrl)
    }

    @Test
    fun `with originalBaseUrl set, the query is preserved`() {
        MultiBaseUrls.setup {
            originalBaseUrl = "https://default.example.com/api/"
            Prod with "https://prod.example.com/v2/"
        }
        val api = prefixedRetrofit.create(PrefixedApi::class.java)

        request { api.withQuery() }

        assertEquals("https://prod.example.com/v2/posts?limit=10", capturedUrl)
    }

    @Test
    fun `invalid originalBaseUrl throws IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            MultiBaseUrls.setup {
                originalBaseUrl = "not a url"
            }
        }
    }

    @Test
    fun `Retrofit Builder extension captures baseUrl and prefixed switching works`() {
        MultiBaseUrls.setup {
            Prod with "https://prod.example.com/v2/"
        }
        val autoRetrofit = Retrofit.Builder()
            .baseUrl("https://default.example.com/api/")
            .client(client)
            .multiBaseUrls()
        val api = autoRetrofit.create(PrefixedApi::class.java)

        request { api.posts() }

        assertEquals("https://prod.example.com/v2/posts", capturedUrl)
    }
}

interface EndToEndApi {
    @BaseUrl(Prod::class)
    @GET("posts")
    fun posts(): Call<okhttp3.ResponseBody>
}

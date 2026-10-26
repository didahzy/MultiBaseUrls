package ziyao.huang.multibaseurls

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

/**
 * Environment markers: any object implementing [Host] can be used as the target of `@BaseUrl`.
 * [PokeApi]'s URL carries a `/v2/` path prefix, demonstrating that prefixes are preserved on switch.
 */
object JsonPlaceholder : Host
object PokeApi : Host

interface SampleApiService {

    /** Example 1: untagged → keeps Retrofit's original baseUrl unchanged. */
    @GET("posts/1")
    suspend fun defaultPost(): Response<Post>

    /** Example 2: method-level `@BaseUrl` → switches to PokeApi, requesting https://pokeapi.co/api/v2/pokemon/ditto . */
    @BaseUrl(PokeApi::class)
    @GET("pokemon/ditto")
    suspend fun pokemon(): Response<Pokemon>

    /** Example 3: `@Ignore` → always uses the original baseUrl, never switches. */
    @Ignore
    @GET("posts/2")
    suspend fun ignoredPost(): Response<Post>

    companion object {
        private val okhttp: OkHttpClient = OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor { msg -> Log.d("OkHttp", msg) }
                    .apply { level = HttpLoggingInterceptor.Level.BODY }
            )
            .multiBaseUrls()
            .build()

        val api: SampleApiService by lazy {
            Retrofit.Builder()
                .baseUrl("https://jsonplaceholder.typicode.com/")
                .client(okhttp)
                .addConverterFactory(GsonConverterFactory.create())
                .multiBaseUrls() // instead of build(): captures the baseUrl above as the original baseUrl
                .create(SampleApiService::class.java)
        }
    }
}

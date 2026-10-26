package ziyao.huang.multibaseurls

import okhttp3.OkHttpClient
import retrofit2.Retrofit

/** Installs the baseUrl-switching interceptor; equivalent to addInterceptor(MultiBaseUrls.interceptor). */
fun OkHttpClient.Builder.multiBaseUrls(): OkHttpClient.Builder =
    addInterceptor(MultiBaseUrls.interceptor)

/** Builds the Retrofit instance and captures its baseUrl as [MultiBaseUrls.originalBaseUrl]; a drop-in replacement for [Retrofit.Builder.build]. */
fun Retrofit.Builder.multiBaseUrls(): Retrofit {
    val retrofit = build()
    MultiBaseUrls.originalBaseUrl = retrofit.baseUrl()
    return retrofit
}

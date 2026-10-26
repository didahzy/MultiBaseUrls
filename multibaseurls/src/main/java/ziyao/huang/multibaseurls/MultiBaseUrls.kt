package ziyao.huang.multibaseurls

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import retrofit2.Invocation
import retrofit2.http.Url
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

object MultiBaseUrls {

    /** Fallback for markers with no registered URL: return a URL string to switch to it, or null to fall back to the original baseUrl. Defaults to null, which throws an exception. */
    var onUnregistered: ((marker: KClass<*>) -> String?)? = null

    /** The original baseUrl of the Retrofit instance. When set, its path prefix is stripped from the request path before resolving onto the target URL; null treats the original baseUrl as root. */
    var originalBaseUrl: HttpUrl? = null

    private val registry = ConcurrentHashMap<KClass<*>, HttpUrl>()

    /** Registers the target URL for a marker type. Invalid URLs throw [IllegalArgumentException] here, not at request time. */
    fun register(marker: KClass<*>, url: String) {
        registry[marker] = url.toHttpUrl()
    }

    fun unregister(marker: KClass<*>) {
        registry.remove(marker)
    }

    fun clear() {
        registry.clear()
    }

    fun urlOf(marker: KClass<*>): String? = registry[marker]?.toString()

    /**
     * Per-request baseUrl switching: reads the [BaseUrl] / [Ignore] annotations through the
     * [Invocation] attached by Retrofit; method-level annotations take precedence over class-level
     * ones. Methods marked [Ignore], or with a [Url] parameter, are never switched.
     */
    val interceptor: Interceptor = Interceptor { chain ->
        val request = chain.request()
        val invocation = request.tag(Invocation::class.java)
            ?: return@Interceptor chain.proceed(request)
        val method = invocation.method()
        val skipSwitch = method.isAnnotationPresent(Ignore::class.java) ||
            method.parameterAnnotations.any { anns -> anns.any { it is Url } }
        if (skipSwitch) return@Interceptor chain.proceed(request)

        val marker = method.getAnnotation(BaseUrl::class.java)?.type
            ?: method.declaringClass.getAnnotation(BaseUrl::class.java)?.type
            ?: return@Interceptor chain.proceed(request)

        val target = targetUrlOf(marker, request)
        if (target != null) {
            chain.proceed(request.newBuilder().url(target).build())
        } else {
            chain.proceed(request)
        }
    }

    /** Resolves the target URL; null means fall back to the original baseUrl per [onUnregistered]. */
    private fun targetUrlOf(marker: KClass<*>, request: Request): HttpUrl? {
        registry[marker]?.let { return retarget(it, request) }
        val handler = onUnregistered
            ?: throw IllegalStateException(
                "MultiBaseUrls: no URL registered for marker ${marker.qualifiedName}. " +
                    "Register it with Marker.baseUrl(\"…\"), or set MultiBaseUrls.onUnregistered."
            )
        return handler(marker)?.toHttpUrl()?.let { retarget(it, request) }
    }

    /**
     * Re-hosts the request: strips the old path prefix according to [originalBaseUrl] and resolves
     * onto the new URL, preserving the new URL's own path prefix. Without [originalBaseUrl] the
     * request path is treated as an absolute path.
     */
    private fun retarget(base: HttpUrl, request: Request): HttpUrl {
        val path = request.url.encodedPath
        val prefix = originalBaseUrl?.encodedPath
        val relativePath = if (prefix != null && path.startsWith(prefix)) path.removePrefix(prefix) else path
        val query = request.url.encodedQuery
        val relativeRef = if (query != null) "$relativePath?$query" else relativePath
        return base.resolve(relativeRef)
            ?: base.newBuilder()
                .encodedPath(request.url.encodedPath)
                .encodedQuery(query)
                .build()
    }
}

package ziyao.huang.multibaseurls

import okhttp3.HttpUrl.Companion.toHttpUrl
import kotlin.reflect.KClass

/** DSL entry point: inside the block, register URLs with `Marker with "https://…"` and configure the fallback policy. */
fun MultiBaseUrls.setup(block: MultiBaseUrlsScope.() -> Unit) {
    MultiBaseUrlsScope().block()
}

@DslMarker
@Target(AnnotationTarget.CLASS)
annotation class MultiBaseUrlsDsl

@MultiBaseUrlsDsl
class MultiBaseUrlsScope @PublishedApi internal constructor() {

    infix fun Host.with(url: String) {
        MultiBaseUrls.register(this::class, url)
    }

    fun onUnregistered(handler: (marker: KClass<*>) -> String?) {
        MultiBaseUrls.onUnregistered = handler
    }

    var originalBaseUrl: String? = null
        set(value) {
            field = value
            MultiBaseUrls.originalBaseUrl = value?.toHttpUrl()
        }
}

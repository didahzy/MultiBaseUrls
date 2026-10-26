package ziyao.huang.multibaseurls

/** Marker interface: implement it on an environment type to register its URL with one line via [baseUrl]. */
interface Host

fun Host.baseUrl(url: String) {
    MultiBaseUrls.register(this::class, url)
}

package ziyao.huang.multibaseurls

import android.app.Application

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Register every environment's URL once; interface methods are then routed by @BaseUrl
        MultiBaseUrls.setup {
            JsonPlaceholder with "https://jsonplaceholder.typicode.com/"
            PokeApi with "https://pokeapi.co/api/v2/"
        }
    }
}

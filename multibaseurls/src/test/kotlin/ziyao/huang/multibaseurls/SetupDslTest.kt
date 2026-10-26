package ziyao.huang.multibaseurls

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

private object DslProd : Host
private object DslTest : Host

class SetupDslTest {

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
    fun `setup DSL registers multiple URLs in one block`() {
        MultiBaseUrls.setup {
            DslProd with "https://prod.example.com"
            DslTest with "https://test.example.com/"
        }

        assertEquals("https://prod.example.com/", MultiBaseUrls.urlOf(DslProd::class))
        assertEquals("https://test.example.com/", MultiBaseUrls.urlOf(DslTest::class))
    }

    @Test
    fun `setup DSL configures the unregistered fallback`() {
        MultiBaseUrls.setup {
            onUnregistered { null }
        }

        assertNotNull(MultiBaseUrls.onUnregistered)
    }

    @Test
    fun `registration inside setup DSL can be overridden afterwards`() {
        MultiBaseUrls.setup {
            DslProd with "https://old.example.com/"
        }

        DslProd.baseUrl("https://new.example.com/")

        assertEquals("https://new.example.com/", MultiBaseUrls.urlOf(DslProd::class))
    }
}

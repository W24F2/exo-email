package com.example.email_client.core.mail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProviderCatalogTest {
    @Test fun gmail() = assertEquals("gmail", ProviderCatalog.detect("a@gmail.com")!!.id)
    @Test fun googlemail() = assertEquals("gmail", ProviderCatalog.detect("a@googlemail.com")!!.id)
    @Test fun qq() = assertEquals("qq", ProviderCatalog.detect("x@qq.com")!!.id)
    @Test fun foxmail() = assertEquals("qq", ProviderCatalog.detect("x@foxmail.com")!!.id)
    @Test fun netease126() = assertEquals("netease163", ProviderCatalog.detect("a@126.com")!!.id)
    @Test fun outlook() = assertEquals("outlook", ProviderCatalog.detect("a@hotmail.com")!!.id)
    @Test fun unknown() = assertNull(ProviderCatalog.detect("a@corp.io"))
    @Test fun caseInsensitive() = assertEquals("gmail", ProviderCatalog.detect("A@GMAIL.COM")!!.id)
    @Test fun noAt() = assertNull(ProviderCatalog.detect("not-an-email"))
    @Test fun byId() = assertEquals("qq", ProviderCatalog.byId("qq")!!.id)
}

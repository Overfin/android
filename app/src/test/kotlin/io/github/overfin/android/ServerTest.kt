package io.github.overfin.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerTest {
    @Test
    fun whatPeopleTypeBecomesAnOrigin() {
        assertEquals("https://example.org", Server.originOf(" Example.org "))
        assertEquals("https://example.org", Server.originOf("https://example.org/title/m1"))
        assertEquals("https://example.org:8443", Server.originOf("example.org:8443"))
    }

    @Test
    fun onlyHttpsAddressesAreAccepted() {
        assertNull(Server.originOf(""))
        assertNull(Server.originOf("http://example.org"))
        assertNull(Server.originOf("ftp://example.org"))
        assertNull(Server.originOf("exa mple.org"))
    }

    @Test
    fun aLogoComesFromTheServerItself() {
        val origin = "https://example.org"
        assertEquals("https://example.org/api/v1/branding/logo?v=1", Server.logoAddress(origin, "/api/v1/branding/logo?v=1"))
        assertNull(Server.logoAddress(origin, ""))
        assertNull(Server.logoAddress(origin, "//other.test/logo"))
        assertNull(Server.logoAddress(origin, "@other.test/logo"))
        assertNull(Server.logoAddress(origin, "https://other.test/logo"))
    }

    @Test
    fun theViewStaysOnItsOwnServer() {
        val origin = "https://example.org"
        assertTrue(Server.sameOrigin("https://example.org/play/m1", origin))
        assertTrue(Server.sameOrigin("https://EXAMPLE.org:443/", origin))
        assertFalse(Server.sameOrigin("http://example.org/", origin))
        assertFalse(Server.sameOrigin("https://example.org.evil.test/", origin))
        assertFalse(Server.sameOrigin("https://example.org:8443/", origin))
        assertFalse(Server.sameOrigin("https://other.test/?x=https://example.org", origin))
        assertFalse(Server.sameOrigin("not a url", origin))
    }
}

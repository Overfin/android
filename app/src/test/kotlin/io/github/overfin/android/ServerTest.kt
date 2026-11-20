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
    fun plainHttpOnlyForAPrivateAddress() {
        assertEquals("http://192.168.1.10", Server.originOf("192.168.1.10"))
        assertEquals("http://10.0.0.12:8090", Server.originOf("http://10.0.0.12:8090/films"))
        assertEquals("http://172.20.1.1", Server.originOf("http://172.20.1.1"))
        assertEquals("http://100.100.1.2", Server.originOf("100.100.1.2"))
        assertEquals("http://[fd12::1]:8080", Server.originOf("http://[fd12::1]:8080"))
        assertEquals("https://192.168.1.10", Server.originOf("https://192.168.1.10"))
        assertNull(Server.originOf("http://8.8.8.8"))
        assertNull(Server.originOf("http://172.32.0.1"))
        assertNull(Server.originOf("http://100.128.0.1"))
        assertNull(Server.originOf("http://192.168.1.300"))
        assertNull(Server.originOf("http://127.0.0.1"))
        assertNull(Server.originOf("http://[2001:db8::1]"))
        assertNull(Server.originOf("http://media.home.arpa"))
        assertNull(Server.originOf("http://10.0.0.1.example.org"))
        assertEquals("https://8.8.8.8", Server.originOf("8.8.8.8"))
    }

    @Test
    fun onlyHttpsAddressesAreAccepted() {
        assertNull(Server.originOf(""))
        assertNull(Server.originOf("http://example.org"))
        assertNull(Server.originOf("ftp://example.org"))
        assertNull(Server.originOf("exa mple.org"))
        assertNull(Server.originOf("example.org:99999"))
        assertNull(Server.originOf("example.org:0"))
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

    @Test
    fun aPlainHttpServerStaysItself() {
        val origin = "http://192.168.1.10"
        assertTrue(Server.sameOrigin("http://192.168.1.10/films", origin))
        assertTrue(Server.sameOrigin("http://192.168.1.10:80/", origin))
        assertFalse(Server.sameOrigin("https://192.168.1.10/", origin))
        assertFalse(Server.sameOrigin("http://192.168.1.10:8090/", origin))
        assertTrue(Server.sameOrigin("http://192.168.1.10:8090/", "http://192.168.1.10:8090"))
    }
}

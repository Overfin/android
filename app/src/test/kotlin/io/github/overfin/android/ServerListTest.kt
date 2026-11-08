package io.github.overfin.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerListTest {
    private val a = ServerEntry("https://a.test", "A")
    private val b = ServerEntry("https://b.test", "B")

    @Test
    fun addingKeepsTheOthersAndBecomesTheLastUsed() {
        val list = ServerList().added(a).added(b)
        assertEquals(listOf(a, b), list.entries)
        assertEquals(b.origin, list.last)
    }

    @Test
    fun addingAgainOnlyUpdatesTheName() {
        val list = ServerList().added(a).added(b).added(ServerEntry(a.origin, "New"))
        assertEquals(listOf(ServerEntry(a.origin, "New"), b), list.entries)
        assertEquals(a.origin, list.last)
    }

    @Test
    fun removingTheLastUsedLeavesNone() {
        val list = ServerList().added(a).added(b).removed(b.origin)
        assertEquals(listOf(a), list.entries)
        assertNull(list.last)
        assertEquals(b.origin, ServerList().added(b).added(a).removed(a.origin).opened(b.origin).last)
    }

    @Test
    fun onlyAServerInTheListOpens() {
        val list = ServerList().added(a)
        assertTrue(a.origin in list)
        assertFalse("https://evil.test" in list)
        assertEquals(a.origin, list.opened("https://evil.test").last)
    }

    @Test
    fun itSurvivesBeingStored() {
        val list = ServerList().added(a).added(b)
        assertEquals(list, ServerList.decode(list.encode()))
        assertEquals(ServerList().added(a).removed(a.origin), ServerList.decode(ServerList().encode()))
    }

    @Test
    fun whatItCantReadIsAnEmptyList() {
        assertEquals(ServerList(), ServerList.decode(null))
        assertEquals(ServerList(), ServerList.decode("not json"))
        assertEquals(ServerList(), ServerList.decode("""{"servers":[{"origin":1}]}"""))
    }

    @Test
    fun aLastUsedOutOfTheListIsIgnored() {
        assertNull(ServerList.decode("""{"servers":[],"last":"https://evil.test"}""").last)
    }
}

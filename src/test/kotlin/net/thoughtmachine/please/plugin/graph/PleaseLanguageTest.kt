package net.thoughtmachine.please.plugin.graph

import junit.framework.TestCase
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

class GraphTest : TestCase() {
    @Test
    fun testParseLabel() {
        assertEquals(BuildLabel("target", "some"), parseLabel("//some:target"))
        assertEquals(BuildLabel("target", "some", "test"), parseLabel("@test//some:target"))
        assertEquals(BuildLabel("target", "some", ""), parseLabel("/////some:target"))
        assertEquals(BuildLabel("target", "some", ""), parseLabel("@//some:target"))
        assertEquals(BuildLabel("some", "some"), parseLabel("//some"))
    }
}
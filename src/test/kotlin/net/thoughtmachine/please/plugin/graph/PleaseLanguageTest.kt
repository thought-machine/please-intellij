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

    @Test
    fun testCanEncodePackage() {
        val pkg = Package(
            pleaseRoot = "someRoot",
            pkg = PackageLabel("some/package"),
            targets = mapOf("//some/package:target" to
                BuildTarget(parseLabel("//some/package:target"), binary = true, test = false,"go_library", listOf("go"))
            )
        )
        val buf = ByteArrayOutputStream()
        val out = DataOutputStream(buf)

        PackageExternalizer.save(out, pkg)
        out.close()
        buf.close()

        val input = DataInputStream(ByteArrayInputStream(buf.toByteArray()))
        val reconstructedPkg = PackageExternalizer.read(input)

        assertEquals(pkg, reconstructedPkg)
    }
}
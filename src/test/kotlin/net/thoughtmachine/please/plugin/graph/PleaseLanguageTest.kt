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
        assertEquals(BuildLabel("target", "some"), BuildLabel.parse("//some:target"))
        assertEquals(BuildLabel("target", "some", "test"), BuildLabel.parse("@test//some:target"))
        assertEquals(BuildLabel("target", "some", ""), BuildLabel.parse("/////some:target"))
        assertEquals(BuildLabel("target", "some", ""), BuildLabel.parse("@//some:target"))
        assertEquals(BuildLabel("some", "some"), BuildLabel.parse("//some"))
    }

    @Test
    fun testCanEncodePackage() {
        val pkg = Package(
            pleaseRoot = "someRoot",
            pkg = PackageLabel("some/package"),
            targets = mapOf(BuildLabel.parse("//some/package:target") to
                BuildTarget(BuildLabel.parse("//some/package:target"), "go_library", listOf("go"))
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
package net.thoughtmachine.please.plugin.subincludes

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.python.psi.PyElementVisitor
import net.thoughtmachine.please.plugin.PleaseFile
import net.thoughtmachine.please.plugin.pleaseFile
import net.thoughtmachine.please.plugin.subinclude.PleaseSubincludeManager
import net.thoughtmachine.please.plugin.subinclude.SubincludeReferenceResolveProvider
import net.thoughtmachine.please.plugin.subinclude.UnresovledSubincludeVisitor
import org.junit.Before
import java.lang.RuntimeException

class SubincludeTest : BasePlatformTestCase() {
    @Before
    fun clearSubincludes() {
        PleaseSubincludeManager.resolvedSubincludes.clear()
    }

    fun testResolveSubinclude(){
        val buildFile = project.pleaseFile(".")
        assertEquals(setOf("//build_defs:test"), buildFile.getSubincludes())

        val buildDefs = PleaseSubincludeManager.resolveSubinclude(buildFile, "//build_defs:test")
        assertEquals(1, buildDefs.size)
        assertEquals("build_defs/test.build_defs", buildDefs.first().path.removePrefix("${buildFile.getProjectRoot().toString()}/"))
    }

    fun testResolveName() {
        val buildFile = project.pleaseFile(".")
        val buildDefs = PleaseSubincludeManager.resolveSubinclude(buildFile, "//build_defs:test")

        val file = PsiManager.getInstance(project).findFile(buildDefs.first())
        assertInstanceOf(file, PleaseFile::class.java)

        // Force the smart cast
        if (file !is PleaseFile) {
            throw RuntimeException()
        }

        val expectedElement = file.findExportedName("test_build_def")
        val actual = SubincludeReferenceResolveProvider.resolveName(buildFile, "test_build_def")
        assertEquals(1, actual.size)
        assertEquals(expectedElement, actual.first())
    }

    fun testSubincludeInspection() {
        val buildFile = project.pleaseFile(".")

        run {
            val holder = ProblemsHolder(InspectionManager.getInstance(project), buildFile, true)
            val visitor = UnresovledSubincludeVisitor(buildFile, holder)


            recursivelyAccept(buildFile, visitor)
            assertEquals(1, holder.results.size)
        }

        run {
            val holder = ProblemsHolder(InspectionManager.getInstance(project), buildFile, true)
            val visitor = UnresovledSubincludeVisitor(buildFile, holder)

            PleaseSubincludeManager.resolveSubinclude(buildFile, "//build_defs:test")
            recursivelyAccept(buildFile, visitor)
            assertEquals(0, holder.results.size)
        }
    }

    // recursivelyAccept calls accept on the provided element and all its children
    private fun recursivelyAccept(element: PsiElement, visitor: PyElementVisitor) {
        element.accept(visitor)
        element.children.forEach {
            recursivelyAccept(it, visitor)
        }
    }

}
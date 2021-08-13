package net.thoughtmachine.please.plugin

import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Test
import java.nio.file.Path

class Test : BasePlatformTestCase() {
    @Test
    fun testPleaseFileLoadsCorrectly() {
        val buildFile = VirtualFileManager.getInstance().findFileByNioPath(Path.of("src/main/testdata"))?.children?.find { it.name == "BUILD" }
        if(buildFile == null) {
            fail("Couldn't find build file")
            return
        }
        val pleaseFile = PsiManager.getInstance(project).findFile(buildFile) as PleaseFile

        assertEquals(pleaseFile.fileType, PleaseBuildFileType)
        assertEquals(pleaseFile.targets().map { it.label }, listOf("//:test"))
        assertEquals(pleaseFile.getProjectRoot().toString(), pleaseFile.parent?.virtualFile?.path)
    }
}
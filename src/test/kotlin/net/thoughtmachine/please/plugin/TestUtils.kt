package net.thoughtmachine.please.plugin

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.lang.RuntimeException
import java.nio.file.Path

fun (Project).pleaseFile(pkg : String): PleaseFile {
    val path = "src/main/testdata/$pkg"
    val buildFile = VirtualFileManager.getInstance().findFileByNioPath(Path.of(path))?.children?.find { it.name == "BUILD" }
    if(buildFile == null) {
        BasePlatformTestCase.fail("Couldn't find build file")
        throw RuntimeException("couldn't find a Please file in $path")
    }
    return PsiManager.getInstance(this).findFile(buildFile) as PleaseFile
}
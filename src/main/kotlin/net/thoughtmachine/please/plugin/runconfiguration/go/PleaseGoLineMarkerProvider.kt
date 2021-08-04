package net.thoughtmachine.please.plugin.runconfiguration.go

import com.goide.GoTypes
import com.goide.execution.testing.GoTestFunctionType
import com.goide.execution.testing.GoTestRunLineMarkerProvider
import com.goide.execution.testing.frameworks.testify.GoTestifySupport
import com.goide.psi.*
import com.goide.psi.impl.GoPsiImplUtil
import com.intellij.execution.Executor
import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunManager
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.execution.process.ProcessHandlerFactory
import com.intellij.icons.AllIcons
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.util.Function
import com.intellij.util.ObjectUtils
import net.thoughtmachine.please.plugin.runconfiguration.PleaseTestExecutor


fun (RunConfiguration).executeTarget(target: String, executor: Executor) {
    val mgr = RunManager.getInstance(project) as RunManagerImpl
    name = "plz run $target"
    val config = RunnerAndConfigurationSettingsImpl(mgr, this)

    mgr.addConfiguration(config)

    ProgramRunnerUtil.executeConfiguration(config, executor)
}

/**
 * Provides gutter icons against go test to test via please
 */
object PleaseGoLineMarkerProvider : RunLineMarkerContributor() {
    private val roots = mutableMapOf<VirtualFile, VirtualFile>()

    override fun getInfo(element: PsiElement): Info? {
        if (element !is LeafPsiElement) {
            return null
        }

        if (element.elementType != GoTypes.IDENTIFIER) {
            return null
        }


        val parent = element.parent
        var test = ""
        if (parent is GoFunctionDeclaration) {
            val functionType = GoTestFunctionType.fromName(parent.name)
            // TODO(jpoole): we could probably handle benchmarks here too
            if (functionType != GoTestFunctionType.TEST) {
                return null
            }
            test = parent.name ?: ""
        } else if(parent is GoMethodDeclaration) {
            if (!GoTestifySupport.isRunnableTestifyMethod(parent)) {
                return null
            }
            val receiverType = GoPsiImplUtil.unwrapPointerIfNeeded(parent.receiverType) ?: return null

            val subTestName = findSuiteTestName(element.containingFile as GoFile, (receiverType.resolve(parent) as GoTypeSpec).name!!)
            test = "$subTestName/${parent.name}"
        } else if(parent is GoSpecType) {
            // Technically we should try and run the correct thing here
            if (!GoTestifySupport.isTestifySuite(parent)) {
                return null
            }
            test = findSuiteTestName(element.containingFile as GoFile, element.text) ?: return null
        } else {
            return null
        }

        val target = resolveFileToTarget(element.containingFile) ?: return null
        return Info(
            AllIcons.Actions.Execute,
            Function { "test $target" },
            PleaseGoAction(element.project, DefaultDebugExecutor.getDebugExecutorInstance(), target, test),
            PleaseGoAction(element.project, PleaseTestExecutor, target, test)
        )
    }


    // This is quite the hack but the testify functions above populate the map and we can fetch the usages here
    private fun findSuiteTestName(file: GoFile, typeName: String) : String? {
        val specTypeName = "${file.packageName}.${typeName}"
        val key = Key.findKeyByName("GO_TESTIFY_SUITE_USAGES") as Key<Map<String, Pair<Long, Set<String>>>>
        return file.getUserData(key)?.get(specTypeName)?.second?.first()
    }

    // findPleaseRoot walks backwards up the virtual file system to find the please root.
    private fun findPleaseRoot(file: VirtualFile) : VirtualFile? {
        if (roots.containsKey(file)) {
            return roots[file]
        }

        var f = file.parent
        while (f != null) {
            f.children.forEach {
                if (it.name == ".plzconfig") {
                    roots[file] = it.parent
                    return it.parent
                }
            }
            f = f.parent
            if (roots.containsKey(f)) {
                roots[file] = roots[f]!!
                return roots[f]
            }
        }
        return null
    }

    // resolveFileToTarget attempts to find the target that takes the given file as input
    private fun resolveFileToTarget(file : PsiFile) : String? {
        val pleaseRoot = findPleaseRoot(file.virtualFile) ?: return null
        val path = pleaseRoot.toNioPath().relativize(file.virtualFile.toNioPath())

        val cmd = GeneralCommandLine("plz query whatinputs $path".split(" "))
        cmd.workDirectory = file.project.guessProjectDir()!!.toNioPath().toFile()
        cmd.withRedirectErrorStream(true)

        val process = ProcessHandlerFactory.getInstance().createProcessHandler(cmd)

        if(process.process.waitFor() == 0) {
            return process.process.inputStream.bufferedReader().lines().findFirst().orElse(null)
        }
        val error = String(process.process.inputStream.readAllBytes())
        Notifications.Bus.notify(Notification("Please", "Failed to update subincludes", error, NotificationType.ERROR))
        return null
    }
}
package net.thoughtmachine.please.plugin.runconfiguration.go

import com.goide.execution.testing.GoTestFunctionType
import com.goide.execution.testing.frameworks.testify.GoTestifySupport
import com.goide.psi.*
import com.goide.psi.impl.GoPsiImplUtil
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessHandlerFactory
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import net.thoughtmachine.please.plugin.pleasecommandline.Please
import net.thoughtmachine.please.plugin.runconfiguration.PleaseTestConfiguration
import net.thoughtmachine.please.plugin.runconfiguration.PleaseTestConfigurationType

object GoTestConfigProducer : LazyRunConfigurationProducer<PleaseTestConfiguration>(){
    private val roots = mutableMapOf<VirtualFile, VirtualFile>()

    override fun getConfigurationFactory(): ConfigurationFactory {
        return PleaseTestConfigurationType.configurationFactories[0]
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


        val cmd = GeneralCommandLine(Please(file.project).query("whatinputs", path.toString()))
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

    // This is quite the hack but the testify functions above populate the map and we can fetch the usages here
    private fun findSuiteTestName(file: GoFile, typeName: String) : String? {
        val specTypeName = "${file.packageName}.${typeName}"
        val key = Key.findKeyByName("GO_TESTIFY_SUITE_USAGES") as Key<Map<String, Pair<Long, Set<String>>>>
        return file.getUserData(key)?.get(specTypeName)?.second?.first()
    }

    private fun getTestName(element: PsiElement) : String? {
        when (val parent = element.parent) {
            is GoFunctionDeclaration -> {
                val functionType = GoTestFunctionType.fromName(parent.name)
                // TODO(jpoole): we could probably handle benchmarks here too
                if (functionType != GoTestFunctionType.TEST) {
                    return null
                }
                return parent.name ?: ""
            }
            is GoMethodDeclaration -> {
                if (!GoTestifySupport.isRunnableTestifyMethod(parent)) {
                    return null
                }
                val receiverType = GoPsiImplUtil.unwrapPointerIfNeeded(parent.receiverType) ?: return null
                val file = element.containingFile ?: return null
                val subTestName = findSuiteTestName(file as GoFile, (receiverType.resolve(parent) as GoTypeSpec).name!!)
                return "$subTestName/${parent.name}"
            }
            is GoSpecType -> {
                // Technically we should try and run the correct thing here
                if (!GoTestifySupport.isTestifySuite(parent)) {
                    return null
                }
                return findSuiteTestName(element.containingFile as GoFile, element.text) ?: return null
            }
            else -> {
                return null
            }
        }
    }

    override fun isConfigurationFromContext(
        configuration: PleaseTestConfiguration,
        context: ConfigurationContext
    ): Boolean {
        val element = context.psiLocation ?: return false
        val target = resolveFileToTarget(element.containingFile) ?: return false
        val test = getTestName(element) ?: return false

        return configuration.args.target == target && configuration.args.tests == test
    }

    override fun setupConfigurationFromContext(
        configuration: PleaseTestConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        if(sourceElement.isNull) {
            return false
        }

        val element = sourceElement.get()
        val file = element.containingFile ?: return false
        val target = resolveFileToTarget(file) ?: return false
        val test = getTestName(element) ?: return false
        val root = findPleaseRoot(file.virtualFile) ?: return false

        configuration.args.target = target
        configuration.args.tests = test
        configuration.args.pleaseRoot = root.path

        configuration.name = "test $target -- $test"

        return true
    }

}
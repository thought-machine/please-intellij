package net.thoughtmachine.please.plugin.runconfiguration

import com.intellij.execution.Executor
import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunManager
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.util.Function
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyStringLiteralExpression
import net.thoughtmachine.please.plugin.PleaseBuildFileType
import net.thoughtmachine.please.plugin.PleaseFile

/**
 * This is the actual action the gutter icons perform which creates and runs a please run configuration for the target.
 */
class PleaseAction(private val project: Project, private val executor : Executor, private val target : String) :
    AnAction({if (executor is PleaseBuildExecutor) "plz build $target" else "plz run $target"}, executor.icon) {
    override fun actionPerformed(e: AnActionEvent) {
        val mgr = RunManager.getInstance(project) as RunManagerImpl
        val runConfig = PleaseRunConfiguration(project, PleaseRunConfigurationType.Factory(PleaseRunConfigurationType()), target)
        runConfig.name = "plz run $target"
        val config = RunnerAndConfigurationSettingsImpl(mgr, runConfig)

        mgr.addConfiguration(config)

        ProgramRunnerUtil.executeConfiguration(config, executor)
    }
}

/**
 * Provides gutter icons against build rules in BUILD files
 */
class PleaseLineMarkerProvider : RunLineMarkerContributor() {

    // getInfo needs to apply the run info to the LeafPsiElement as that's what intellij demands. It looks for the
    // IDENT of the function call and applies the run actions to that.
    override fun getInfo(element: PsiElement): Info? {
        //TODO(jpoole): check if we're inspecting a BUILD file or a .build_defs file. We're wasting our time for
        //  build_def files as they don't contain build targets.
        if(element !is LeafPsiElement) {
            return null
        }

        if(element.elementType != PyTokenTypes.IDENTIFIER) {
            return null
        }

        val callExpr = element.parent.parent
        if (callExpr !is PyCallExpression) {
            return null
        }

        val file = element.containingFile
        if(file.fileType != PleaseBuildFileType) {
            return null
        }
        if(file !is PleaseFile) {
            return null
        }

        val name = callExpr.argumentList?.getKeywordArgument("name")
        if (name != null) {
            val expr = name.valueExpression
            if(expr is PyStringLiteralExpression) {
                if(file.getPleasePackage() == null) {
                    return null
                }
                val target = "//${file.getPleasePackage()}:${expr.stringValue}"
                return Info(
                    AllIcons.Actions.Execute, Function { "run $target" },
                    PleaseAction(element.project, DefaultRunExecutor.getRunExecutorInstance(), target),
                    PleaseAction(element.project, DefaultDebugExecutor.getDebugExecutorInstance(), target),
                    PleaseAction(element.project, PleaseBuildExecutor, target)
                )
            }
        }

        return null
    }
}
package net.thoughtmachine.please.plugin.runconfiguration

import com.intellij.execution.Executor
import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunManager
import com.intellij.execution.configurations.RunConfiguration
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


fun (RunConfiguration).executeTarget(target: String, executor: Executor) {
    val mgr = RunManager.getInstance(project) as RunManagerImpl
    name = "plz run $target"
    val config = RunnerAndConfigurationSettingsImpl(mgr, this)

    mgr.addConfiguration(config)

    ProgramRunnerUtil.executeConfiguration(config, executor)
}

/**
 * This is the actual action the gutter icons perform which creates and runs a please run configuration for the target.
 */
class PleaseAction(private val project: Project, private val executor : Executor, private val target : String, plzAction:String) :
    AnAction({"plz $plzAction $target"}, executor.icon) {
    override fun actionPerformed(e: AnActionEvent) {
        PleaseRunConfiguration(project, PleaseRunConfigurationType.Factory(PleaseRunConfigurationType()), target)
            .executeTarget(target, executor)
    }
}

/**
 * Provides gutter icons against build rules in BUILD files
 */
object PleaseLineMarkerProvider : RunLineMarkerContributor() {
    val actionProducers = mutableListOf<(Project, String) -> Collection<AnAction>>()

    // getInfo needs to apply the run info to the LeafPsiElement as that's what intellij demands. It looks for the
    // IDENT of the function call and applies the run actions to that.
    override fun getInfo(element: PsiElement): Info? {
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
        // Skip build defs as they don't define build rules
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
                    AllIcons.Actions.Execute,
                    Function { "run $target" },
                    *actionProducers.flatMap { it(element.project, target) }.toTypedArray()
                )
            }
        }

        return null
    }
}
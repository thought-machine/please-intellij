package net.thoughtmachine.please.plugin.runconfiguration

import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyStringLiteralExpression
import net.thoughtmachine.please.plugin.PleaseBuildFileType
import net.thoughtmachine.please.plugin.PleaseFile
import net.thoughtmachine.please.plugin.graph.resolveTarget


/**
 * Provides gutter icons against build rules in BUILD files
 */
object PleaseLineMarkerProvider : RunLineMarkerContributor() {

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
                    filterGoActions(element, target).toTypedArray(),
                ) { "run $target" }
            }
        }

        return null
    }

    private fun filterGoActions(element: PsiElement, target: String): List<AnAction> {
        val actions = mutableListOf(PleaseAction(element.project, target, "build",
            PleaseBuildExecutor, newBuildConfig(element.project, target)))

        // TODO(jscott) we should find a way of making this more generic for other languages
        if (element.text == "go_binary") {
            actions.addAll(listOf(
                PleaseAction(element.project, target, "run",
                    DefaultRunExecutor.getRunExecutorInstance(), newRunConfig(element.project, target)),
                PleaseAction(element.project, target, "run",
                    DefaultDebugExecutor.getDebugExecutorInstance(), newRunConfig(element.project, target)),
            ))
        } else if (element.text == "go_test") {
            actions.addAll(listOf(
                PleaseAction(element.project, target, "test",
                    DefaultRunExecutor.getRunExecutorInstance(), newTestConfig(element.project, target)),
                PleaseAction(element.project, target, "test",
                    DefaultDebugExecutor.getDebugExecutorInstance(), newTestConfig(element.project, target)),
            ))
        }
        return actions
    }

    private fun newRunConfig(project: Project, target: String) = PleaseRunConfiguration(
        project,
        PleaseRunConfigurationType.Factory(PleaseRunConfigurationType()),
        PleaseRunConfigArgs(resolveTarget(project, target))
    )

    private fun newBuildConfig(project: Project, target: String) = PleaseBuildConfiguration(
        project,
        PleaseBuildConfigurationType.Factory(PleaseBuildConfigurationType()),
        PleaseBuildConfigArgs(target)
    )

    fun newTestConfig(project: Project, target: String, tests: String = "") = PleaseTestConfiguration(
        project,
        PleaseTestConfigurationType.Factory(PleaseTestConfigurationType()),
        PleaseTestConfigArgs(resolveTarget(project, target), tests=tests)
    )

}
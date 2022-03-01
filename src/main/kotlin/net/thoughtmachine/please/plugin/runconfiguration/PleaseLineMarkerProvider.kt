package net.thoughtmachine.please.plugin.runconfiguration

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.actions.RunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyStringLiteralExpression
import net.thoughtmachine.please.plugin.PleaseBuildFileType
import net.thoughtmachine.please.plugin.PleaseFile
import net.thoughtmachine.please.plugin.graph.BuildTarget


/**
 * Provides gutter icons against build rules in BUILD files
 */
object PleaseLineMarkerProvider : RunLineMarkerContributor() {

    // getInfo needs to apply the run info to the LeafPsiElement as that's what intellij demands. It looks for the
    // IDENT of the function call and applies the run actions to that.
    override fun getInfo(element: PsiElement): Info? {
        val file = element.containingFile
        // Skip build defs as they don't define build rules
        if(file.fileType != PleaseBuildFileType) {
            return null
        }
        if(file !is PleaseFile) {
            return null
        }

        val psiTarget = file.targetForIdent(element) ?: return null


        return Info(
            AllIcons.Actions.Execute,
            ExecutorAction.getActions(0),
        ) { "run :${psiTarget.name}" }
    }
}

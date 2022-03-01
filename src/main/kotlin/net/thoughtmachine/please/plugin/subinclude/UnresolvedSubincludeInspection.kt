package net.thoughtmachine.please.plugin.subinclude

import com.intellij.codeInspection.*
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessHandlerFactory
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.psi.PsiElementVisitor
import com.intellij.util.castSafelyTo
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.PyStringLiteralExpression
import net.thoughtmachine.please.plugin.PleaseFile

object UnresolvedSubincludeInspection : LocalInspectionTool() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession
    ): PsiElementVisitor {
        if (holder.file !is PleaseFile) {
            return super.buildVisitor(holder, isOnTheFly, session)
        }
        return UnresovledSubincludeVisitor(holder.file as PleaseFile, holder)
    }
}

class UnresovledSubincludeVisitor(private val file: PleaseFile, private val holder: ProblemsHolder) : PyElementVisitor() {
    override fun visitPyCallExpression(call: PyCallExpression) {
        val functionName = call.callee?.name ?: ""
        if(functionName != "subinclude"){
            return
        }
        val includes = call.arguments.asSequence()
            .map { it.castSafelyTo<PyStringLiteralExpression>() }.filterNotNull()
            .toList()

        includes.asSequence()
            .map { it.castSafelyTo<PyStringLiteralExpression>() }.filterNotNull()
            .forEach { expr ->
                if(!PleaseSubincludeManager.resolvedSubincludes.containsKey(expr.stringValue)) {
                    holder.registerProblem(UnresolvedSubincludeDescriptor(file, expr, includes.map { it.stringValue }))
                }
            }
    }
}

class UnresolvedSubincludeDescriptor(file: PleaseFile, paramExpr: PyStringLiteralExpression, includes: List<String>) : ProblemDescriptorBase(
    paramExpr,
    paramExpr,
    "Unresolved subinclude ${paramExpr.stringValue}",
    arrayOf(ResolveSubincludeQuickFix(file, includes)),
    ProblemHighlightType.ERROR,
    false,
    paramExpr.stringValueTextRange,
    true,
    true
)

class ResolveSubincludeQuickFix(private val file: PleaseFile, private val includes : List<String>) : LocalQuickFix {
    override fun getFamilyName(): String {
        return "Please: resolve subincludes"
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        ProgressManager.getInstance().run(ResolveSubincludeBackgroundTask(file, includes))
    }
}

class ResolveSubincludeBackgroundTask(private var file: PleaseFile, private var includes: List<String>) : Task.Backgroundable(file.project, "Update subincludes") {
    override fun run(indicator: ProgressIndicator) {
        try {
            if(includes.isEmpty()) {
                return
            }

            if (DumbService.getInstance(project).isDumb) {
                Notifications.Bus.notify(Notification("Please", "Cannot update subincludes while indexing", NotificationType.ERROR))
                return
            }


            indicator.text = "Updating subincludes"
            indicator.text2 = "plz build ${includes.joinToString(" ")}"


            val args = arrayOf("plz", "build") + includes
            val cmd = GeneralCommandLine(*args)
            cmd.isRedirectErrorStream = true

            cmd.setWorkDirectory(file.virtualFile.parent.path)
            val process = ProcessHandlerFactory.getInstance().createProcessHandler(cmd)

            if(process.process.waitFor() == 0) {
                includes.forEach{
                    indicator.text2 = "plz query outputs $it"
                    PleaseSubincludeManager.resolveSubinclude(file, it)
                }
            } else {
                val error = String(process.process.inputStream.readAllBytes())
                Notifications.Bus.notify(Notification("Please", "Failed to update subincludes", error, NotificationType.ERROR))
            }
        } catch (_: IndexNotReadyException) {
            // Ignore this as it's a race condition with the check above which happens occasionally. Re-focusing the
            // file will re-trigger this though
        }

    }
}
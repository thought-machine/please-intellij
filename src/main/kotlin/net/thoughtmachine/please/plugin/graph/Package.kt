package net.thoughtmachine.please.plugin.graph

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessHandlerFactory
import com.intellij.openapi.project.Project
import net.thoughtmachine.please.plugin.PleaseFile
import net.thoughtmachine.please.plugin.pleasecommandline.Please

data class BuildLabel(val name: String, val pkg: String, val subrepo: String? = null) {
    override fun toString(): String {
        val label = "//$pkg:$name"
        if (subrepo != null) {
            return "///$subrepo$label"
        }
        return label
    }
}


fun parseLabel(l: String) : BuildLabel {
    val (subrepo, rest) = if (l.startsWith("///") || l.startsWith("@")) {
        val rest = l.removePrefix("///").removePrefix("@")
        val s = rest.substringBefore("//")
        Pair(s, rest.removePrefix(s))
    } else {
        Pair(null, l)
    }

    val (pkg, name) = if (rest.contains(":")) {
        val parts = rest.removePrefix("//").split(":")
        Pair(parts[0], parts[1])
    } else {
        Pair(rest.removePrefix("//"), rest.substringAfterLast("/"))
    }

    return BuildLabel(name, pkg, subrepo)
}

/**
 * TargetInfo contains information about the target from the build system
 */
data class TargetInfo(
    val name: String,
    val binary: Boolean,
    val test: Boolean,
    val labels: List<String> = emptyList()
)

data class BuildTarget(val label: BuildLabel, val pkg: Package, val info: TargetInfo?, val kind: String) {
    override fun toString(): String {
        return label.toString()
    }
}

data class PackageLabel(val name: String, val subrepo: String? = null) {
    override fun toString(): String {
        if (subrepo == null) {
            return "//$name:all"
        }
        return "///$subrepo//$name"
    }
}

data class Package(val project: Project, val file: PleaseFile, val pleaseRoot : String, val pkgLabel: PackageLabel) {

    fun targetByName(name: String) : BuildTarget? {
        val psiTarget = file.targets().firstOrNull{it.name == name} ?: return null
        val info = getTargetInfo(name)
        return BuildTarget(BuildLabel(name, pkgLabel.name), this, info, psiTarget.kind())
    }

    // getPackageTargetInfo uses plz query print --json to get some information about the targets in this package.
    private fun getTargetInfo(name: String): TargetInfo? {
        try {
            val label = "//${pkgLabel.name}:$name"
            val plzCmd = Please(project, true).query("print", "--json", "--omit_hidden", "--field=name", "--field=labels", "--field=test", "--field=binary", label)
            val cmd = GeneralCommandLine(plzCmd).withWorkDirectory(pleaseRoot)
            val process = ProcessHandlerFactory.getInstance().createProcessHandler(cmd)

            val exitCode = process.process.waitFor()
            if (exitCode == 0) {
                return mapper.readValue<Map<String, TargetInfo>>(process.process.inputStream.readAllBytes())[label]
            } else {
                val error = String(process.process.inputStream.readAllBytes())
                throw RuntimeException("Command `${plzCmd.joinToString(" ")}` failed:\nExit code: $exitCode\n$error")
            }
        } catch (e :Exception) {
            return null
        }
    }

    companion object {
        private val mapper: ObjectMapper = ObjectMapper().registerModule(
            KotlinModule.Builder().configure(KotlinFeature.NullToEmptyCollection, true).build()
        ).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }
}

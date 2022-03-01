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

data class PackageLabel(val pkg: String, val subrepo: String? = null) {
    override fun toString(): String {
        if (subrepo == null) {
            return "//$pkg:all"
        }
        return "///$subrepo//$pkg"
    }
}

data class Package(val pleaseRoot : String, val pkg: PackageLabel, val targets: MutableMap<String, BuildTarget> = mutableMapOf()) {
    fun addTarget(t: BuildTarget) {
        targets[t.toString()] = t
    }
    fun targetByName(name: String) : BuildTarget? {
        return targets[BuildLabel(name, pkg.pkg, pkg.subrepo).toString()]
    }
}

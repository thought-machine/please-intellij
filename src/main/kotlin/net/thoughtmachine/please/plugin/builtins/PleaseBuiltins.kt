package net.thoughtmachine.please.plugin.builtins

import com.intellij.openapi.vfs.VfsUtil
import com.intellij.util.ResourceUtil

object PleaseBuiltins {
    private val builtinFiles = setOf("builtins", "c_rules", "cc_rules", "config_rules", "go_rules", "java_rules", "misc_rules", "proto_rules", "python_rules", "sh_rules", "subrepo_rules")
    val PLEASE_BUILTINS by lazy {
        builtinFiles.map {
            VfsUtil.findFileByURL(
                ResourceUtil.getResource(
                    this::class.java.classLoader,
                    "builtins",
                    "$it.build_defs"
                )!!
            )!!
        }
    }
}
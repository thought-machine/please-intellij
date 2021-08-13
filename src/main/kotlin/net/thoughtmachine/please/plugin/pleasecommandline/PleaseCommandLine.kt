package net.thoughtmachine.please.plugin.pleasecommandline

import com.intellij.openapi.project.Project
import net.thoughtmachine.please.plugin.settings.PleaseProjectConfigurable


typealias PleaseCommand = List<String>

class Please(
    private val project: Project,
    private val verbosity: String = "info",
    private val plainOutput : Boolean = true,
    private val config : String = "dbg",
    private val pleaseArgs: List<String> = emptyList()
) {
    fun build(target: String) : PleaseCommand = args() + listOf("build", target)
    fun run(target: String) : PleaseCommand = args() + listOf("run", target)
    fun test(target: String, tests: String) : PleaseCommand = args() + listOf("test", target, "--", tests)
    fun exec(target: String, execCmd: List<String>, shareNetwork : Boolean = true) : PleaseCommand {
        val args = args().toMutableList()
        args.addAll(listOf("exec", target))
        if (shareNetwork) {
            args.add("--share_network")
        }
        args.add("--")
        args.addAll(execCmd)
        return args
    }

    fun args() : List<String> {
        val plz = PleaseProjectConfigurable.getPleasePath(project)
        val args = mutableListOf(plz, "-v", verbosity, "-c", config)
        if(plainOutput) {
            args.add("-p")
        }
        args.addAll(pleaseArgs)
        return args
    }

}

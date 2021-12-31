package net.thoughtmachine.please.plugin.pleasecommandline

import com.intellij.openapi.project.Project
import net.thoughtmachine.please.plugin.settings.PleaseProjectConfigurable


typealias PleaseCommand = List<String>

class Please(
    private val project: Project,
    private val verbosity: String = "warning",
    private val plainOutput : Boolean = true,
    private val config : String = "dbg",
    private val pleaseArgs: List<String> = emptyList()
) {
    fun version() : PleaseCommand = args() + listOf("--version")
    fun build(target: String) : PleaseCommand = args() + listOf("build", target)
    fun test(target: String, tests: String) : PleaseCommand = args() + listOf("test", target, "--", tests)

    fun run(
        target: String,
        inTmpDir: Boolean = false,
        cmd: List<String> = emptyList(),
        programArgs: List<String> = emptyList()
    ) : PleaseCommand {
        val args = (args() + listOf("run", target)).toMutableList()
        if (inTmpDir) {
            args.add("--in_tmp_dir")
        }
        if (cmd.isNotEmpty()) {
            args.addAll(listOf("--cmd", cmd.joinToString(" ")))
        }
        if (programArgs.isNotEmpty()) {
            args.add("--")
            args.addAll(programArgs)
        }
        return args
    }

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

    fun query(subcommand: String, arguments: Array<String>) : PleaseCommand {
        val args = args().toMutableList()
        args.addAll(listOf("query", subcommand, *arguments))
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

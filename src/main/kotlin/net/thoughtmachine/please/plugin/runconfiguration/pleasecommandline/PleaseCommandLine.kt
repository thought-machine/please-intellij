package net.thoughtmachine.please.plugin.runconfiguration.pleasecommandline


typealias PleaseCommand = List<String>

class Please(
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
        val args = mutableListOf("plz", "-v", verbosity, "-c", config)
        if(plainOutput) {
            args.add("-p")
        }
        args.addAll(pleaseArgs)
        return args
    }

}

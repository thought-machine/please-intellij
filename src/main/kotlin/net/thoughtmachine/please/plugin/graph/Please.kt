package net.thoughtmachine.please.plugin.graph

data class BuildLabel(val name: String, val pkg: String, val subrepo: String? = null) {
    override fun toString(): String {
        val label = "//$pkg:$name"
        if (subrepo != null) {
            return "///$subrepo$label"
        }
        return label
    }

    companion object {
        fun parse(l: String) : BuildLabel {
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
    }
}

data class BuildTarget(val label: BuildLabel, val kind: String, val labels: List<String>) {
    override fun toString(): String {
        return label.toString()
    }

    companion object {
        fun of(label: String) : BuildTarget {
            return BuildTarget(BuildLabel.parse(label), "", listOf())
        }
    }
}

data class PackageLabel(val pkg: String, val subrepo: String? = null) {
    override fun toString(): String {
        if (subrepo == null) {
            return "//$pkg:all"
        }
        return "///$subrepo//$pkg"
    }

    companion object {
        fun parse(l : String) : PackageLabel {
           val label = BuildLabel.parse(l)
           return PackageLabel(label.pkg, label.subrepo)
        }
    }


}

data class Package(val pleaseRoot : String, val pkg: PackageLabel, val targets: Map<BuildLabel, BuildTarget>)

data class BuildGraph(
    private val packages: MutableMap<PackageLabel, Package>,
    private val targets: MutableMap<BuildLabel, BuildTarget>
)

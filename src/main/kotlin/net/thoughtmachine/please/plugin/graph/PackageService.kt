package net.thoughtmachine.please.plugin.graph

import com.intellij.openapi.project.Project
import com.intellij.util.SlowOperations
import com.intellij.util.indexing.FileBasedIndex

class PackageService(val root: String) {
    private val pkgs = mutableMapOf<String, Package?>()

    fun resolvePackage(project: Project, pkg: String): Package? {
        SlowOperations.assertSlowOperationsAreAllowed()
        val file = PackageIndexer.lookup(project, root, pkg) ?: return null
        return pkgs.getOrPut(pkg) {
            return Package(project, file, root, PackageLabel(pkg))
        }
    }

    companion object {
        private val instances = mutableMapOf<String, PackageService>()
        private val allRoots = mutableMapOf<Project, List<String>>()

        fun getInstance(pleaseRoot: String) : PackageService {
            return instances.getOrPut(pleaseRoot) {
                PackageService(pleaseRoot)
            }
        }

        fun resolvePackage(project: Project, root: String, pkg: String): Package? {
            return getInstance(root).resolvePackage(project, pkg)
        }

        fun resolvePackage(project: Project, pkg: String) : List<Package> {
            return allRoots.getOrPut(project) {
                FileBasedIndex.getInstance().getAllKeys(PackageIndexExtension.name, project).map { it.first }
            }.mapNotNull { resolvePackage(project, it, pkg) }
        }
    }
}
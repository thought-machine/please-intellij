package net.thoughtmachine.please.plugin.runconfiguration.go;

import com.goide.dlv.location.DefaultDlvPositionConverter
import com.goide.dlv.location.DlvPositionConverter;
import com.goide.dlv.location.DlvPositionConverterFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import net.thoughtmachine.please.plugin.graph.PackageService
import java.nio.file.Path

object PositionConverter : DlvPositionConverterFactory {
    override fun createPositionConverter(project: Project, module: Module?, sourceSet: MutableSet<String>): DlvPositionConverter {
        return PleasePositionConverter(sourceSet,
            DlvPositionConverter.Caching(DefaultDlvPositionConverter(project, sourceSet))
        )
    }
}

class PleasePositionConverter(private val sourceSet: MutableSet<String>, private val delegate: DlvPositionConverter) : DlvPositionConverter {
    private val mapping = mutableMapOf<String, VirtualFile>()
    override fun toRemotePath(file: VirtualFile): String? {
        val res = delegate.toRemotePath(file) ?: sourceSet.filter { file.path.endsWith(it) }.maxByOrNull { it.length } ?: return null
        mapping[res] = file
        return res
    }

    override fun toLocalFile(file: String): VirtualFile? {
        return delegate.toLocalFile(file) ?: mapping[file]
    }
}

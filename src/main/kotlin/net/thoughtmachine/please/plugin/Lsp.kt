package net.thoughtmachine.please.plugin

import com.intellij.openapi.application.PreloadingActivity
import com.intellij.openapi.progress.ProgressIndicator
import org.wso2.lsp4intellij.IntellijLanguageClient
import org.wso2.lsp4intellij.client.languageserver.serverdefinition.RawCommandServerDefinition

/**
 * Registers the lsp definition for the Pleaes file type
 */
class PleaseLSPPreloadActivity : PreloadingActivity() {
    override fun preload(indicator: ProgressIndicator) {
        //TODO(jpoole): use `plz tool lsp`
        IntellijLanguageClient.addServerDefinition(
            PleaseFileType,
            RawCommandServerDefinition(
                "plz",
                arrayOf("plz", "tool", "langserver")
            )
        )
    }
}
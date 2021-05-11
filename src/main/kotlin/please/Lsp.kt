package please

import com.intellij.openapi.application.PreloadingActivity
import com.intellij.openapi.progress.ProgressIndicator
import org.wso2.lsp4intellij.IntellijLanguageClient
import org.wso2.lsp4intellij.client.languageserver.serverdefinition.RawCommandServerDefinition

class PleaseLSPPreloadActivity : PreloadingActivity() {
    override fun preload(indicator: ProgressIndicator) {
        //TODO(jpoole): use `plz tool lsp`
        IntellijLanguageClient.addServerDefinition(
            PleaseFileType,
            RawCommandServerDefinition(
                "plz",
                arrayOf("${System.getProperty("user.home")}/.please/build_langserver")
            )
        )
    }
}
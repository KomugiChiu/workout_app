package com.komugi.workout.util

import android.annotation.SuppressLint
import android.content.Intent
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.commonmark.ext.autolink.AutolinkExtension
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer

object MarkdownRenderer {

    private val extensions = listOf(TablesExtension.create(), AutolinkExtension.create())
    private val parser = Parser.builder().extensions(extensions).build()
    private val renderer = HtmlRenderer.builder().extensions(extensions).build()

    fun toHtml(markdown: String): String {
        val body = renderer.render(parser.parse(markdown))
        return """
            <!DOCTYPE html>
            <html>
            <head>
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <style>
            body { font-family: -apple-system, Roboto, sans-serif; font-size: 15px; line-height: 1.65; color: #1c1b1f; padding: 16px 18px 56px; margin: 0; }
            h1 { font-size: 22px; border-bottom: 2px solid #2E7D32; padding-bottom: 8px; }
            h2 { font-size: 18px; margin-top: 28px; color: #2E7D32; }
            h3 { font-size: 16px; margin-top: 20px; }
            table { border-collapse: collapse; width: 100%; margin: 12px 0; font-size: 13px; }
            th, td { border: 1px solid #d0d0d0; padding: 6px 8px; text-align: left; vertical-align: top; }
            th { background: #f1f5f0; }
            code { background: #f2f2f2; padding: 1px 4px; border-radius: 4px; font-size: 13px; }
            pre { background: #f6f6f6; padding: 12px; border-radius: 8px; overflow-x: auto; }
            blockquote { border-left: 4px solid #A5D6A7; margin: 12px 0; padding: 4px 12px; color: #444; background: #fafafa; }
            a { color: #1565C0; }
            ul, ol { padding-left: 22px; }
            hr { border: none; border-top: 1px solid #e0e0e0; margin: 20px 0; }
            </style>
            </head>
            <body>$body</body>
            </html>
        """.trimIndent()
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MarkdownWebView(markdown: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val html = remember(markdown) { MarkdownRenderer.toHtml(markdown) }
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val url = request?.url ?: return false
                        return runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, url))
                            true
                        }.getOrDefault(true)
                    }
                }
                settings.javaScriptEnabled = false
                setBackgroundColor(android.graphics.Color.WHITE)
            }
        },
        update = { it.loadDataWithBaseURL(null, html, "text/html", "utf-8", null) }
    )
}

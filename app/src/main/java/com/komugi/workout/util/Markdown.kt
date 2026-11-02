package com.komugi.workout.util

import android.annotation.SuppressLint
import android.content.Intent
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import java.util.Locale
import org.commonmark.ext.autolink.AutolinkExtension
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer

/**
 * 動作說明內文使用的配色，全部取自目前 Material 主題，
 * 因此三種主題色與淺／深色模式都會即時反映在文章上。
 */
data class MarkdownPalette(
    val background: Color,
    val onBackground: Color,
    val primary: Color,
    val onSurfaceVariant: Color,
    val surfaceVariant: Color,
    val outline: Color
) {
    companion object {
        @Composable
        fun fromTheme(): MarkdownPalette = MarkdownPalette(
            background = MaterialTheme.colorScheme.background,
            onBackground = MaterialTheme.colorScheme.onBackground,
            primary = MaterialTheme.colorScheme.primary,
            onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant,
            surfaceVariant = MaterialTheme.colorScheme.surfaceVariant,
            outline = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

private fun Color.toCssHex(): String =
    String.format(Locale.US, "#%06X", toArgb() and 0xFFFFFF)

object MarkdownRenderer {

    private val extensions = listOf(TablesExtension.create(), AutolinkExtension.create())
    private val parser = Parser.builder().extensions(extensions).build()
    private val renderer = HtmlRenderer.builder().extensions(extensions).build()

    fun toHtml(markdown: String, palette: MarkdownPalette): String {
        val body = renderer.render(parser.parse(markdown))
        val background = palette.background.toCssHex()
        val onBackground = palette.onBackground.toCssHex()
        val primary = palette.primary.toCssHex()
        val onSurfaceVariant = palette.onSurfaceVariant.toCssHex()
        val surfaceVariant = palette.surfaceVariant.toCssHex()
        val outline = palette.outline.toCssHex()

        return """
            <!DOCTYPE html>
            <html>
            <head>
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <style>
            html, body { background: $background; }
            body { font-family: -apple-system, Roboto, sans-serif; font-size: 15px; line-height: 1.65; color: $onBackground; padding: 16px 18px 56px; margin: 0; overflow-wrap: break-word; }
            h1 { font-size: 22px; border-bottom: 2px solid $primary; padding-bottom: 8px; }
            h2 { font-size: 18px; margin-top: 28px; color: $primary; }
            h3 { font-size: 16px; margin-top: 20px; }
            table { border-collapse: collapse; width: 100%; margin: 12px 0; font-size: 13px; }
            th, td { border: 1px solid $outline; padding: 6px 8px; text-align: left; vertical-align: top; }
            th { background: $surfaceVariant; }
            code { background: $surfaceVariant; padding: 1px 4px; border-radius: 4px; font-size: 13px; }
            pre { background: $surfaceVariant; padding: 12px; border-radius: 8px; overflow-x: auto; }
            pre code { background: transparent; padding: 0; }
            blockquote { border-left: 4px solid $primary; margin: 12px 0; padding: 4px 12px; color: $onSurfaceVariant; background: $surfaceVariant; border-radius: 0 8px 8px 0; }
            a { color: $primary; }
            ul, ol { padding-left: 22px; }
            hr { border: none; border-top: 1px solid $outline; margin: 20px 0; }
            img { max-width: 100%; height: auto; }
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
    val palette = MarkdownPalette.fromTheme()
    val html = remember(markdown, palette) { MarkdownRenderer.toHtml(markdown, palette) }

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
            }
        },
        update = { webView ->
            // 只在內容或主題配色改變時重新載入，避免每次重组都重置捲動位置。
            if (webView.tag != html) {
                webView.tag = html
                webView.setBackgroundColor(palette.background.toArgb())
                webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
            }
        }
    )
}

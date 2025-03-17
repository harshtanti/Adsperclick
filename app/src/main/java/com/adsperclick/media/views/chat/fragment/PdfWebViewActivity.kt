package com.adsperclick.media.views.chat.fragment

import android.os.Bundle
import android.webkit.WebView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.adsperclick.media.R
import java.net.URLEncoder

class PdfWebViewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pdf_web_view)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
        }

        setContentView(webView)

        intent.getStringExtra("pdf_url")?.let { url ->
            webView.loadUrl("https://docs.google.com/viewer?url=${URLEncoder.encode(url, "UTF-8")}&embedded=true")
            // Or alternatively: webView.loadUrl(url)
        }
    }
}
    // NOTES :
    // Yes, WebView can handle various document types beyond just PDFs, especially
    // when using the Google Docs viewer approach. The line:
    /*

    This leverages Google Docs Viewer, which supports multiple document formats including:
    PDF (.pdf)
    Word documents (.doc, .docx)
    Excel spreadsheets (.xls, .xlsx)
    PowerPoint presentations (.ppt, .pptx)
    TIFF images (.tiff)
    Plain text files (.txt)
    Some image formats (.jpg, .png)

    For most of these formats, you won't need to make any changes to your code. The Google Docs viewer
    * will automatically detect the file type and render it appropriately.
    However, there are a few limitations to keep in mind:

    There's a file size limit (around 25MB for Google Docs viewer)
    It requires an internet connection to work
    Some complex formatting might not render perfectly in all document types
    For very large files, loading might be slow

    If you later need more specialized handling for specific document types, you might want to look
    * into dedicated libraries for those formats. But for most common use cases, the WebView with
    *  Google Docs viewer should work well across different document types.
    *
    * */
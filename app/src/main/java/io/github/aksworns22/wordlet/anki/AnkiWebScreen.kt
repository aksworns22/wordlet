package io.github.aksworns22.wordlet.anki

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import io.github.aksworns22.wordlet.R

private const val SHARED_DECKS_URL = "https://ankiweb.net/shared/decks"

/** 덱 상세 페이지의 경로. */
private val DECK_PAGE_PATH = Regex("^/shared/info/\\d+/?$")

/**
 * 페이지의 Download 버튼을 대신 누른다. 화면이 아직 그려지는 중일 수 있어 버튼이 나올 때까지 잠깐 기다린다.
 * 문구가 바뀌어도 찾도록 덱 페이지의 큰 주 버튼으로도 찾는다.
 */
private const val CLICK_DOWNLOAD_SCRIPT = """
(function() {
  var tries = 0;
  var timer = setInterval(function() {
    var buttons = Array.prototype.slice.call(document.querySelectorAll('button'));
    var button = buttons.find(function(b) { return /download/i.test(b.textContent); })
      || document.querySelector('button.btn-primary.btn-lg');
    if (button || ++tries > 50) clearInterval(timer);
    if (button) button.click();
  }, 100);
})();
"""

/**
 * 앱 안에서 AnkiWeb 공유 덱을 둘러보는 화면.
 * 덱 페이지에서 Download를 누르면 [onDownload]로 넘겨 앱이 직접 받는다. AnkiWeb 밖의 링크는 브라우저로 연다.
 * 덱 페이지에서는 Download를 찾지 않아도 되도록 아래에 가져오기 버튼을 띄운다.
 */
@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AnkiWebScreen(
    onDownload: (url: String, userAgent: String, contentDisposition: String?, mimeType: String?) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val currentOnDownload by rememberUpdatedState(onDownload)
    var progress by remember { mutableIntStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var onDeckPage by remember { mutableStateOf(false) }
    val webView =
        remember {
            WebView(context).apply {
                // AnkiWeb은 자바스크립트로 그리는 SvelteKit 앱이다.
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient =
                    object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest
                        ): Boolean {
                            // 다운로드는 다른 호스트로 리다이렉트될 수 있어 리다이렉트는 그대로 따라간다.
                            if (request.isRedirect || request.url.host?.endsWith("ankiweb.net") == true) return false
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, request.url)) }
                            return true
                        }

                        override fun onPageStarted(
                            view: WebView,
                            url: String?,
                            favicon: Bitmap?
                        ) {
                            canGoBack = view.canGoBack()
                            onDeckPage = isDeckPage(url)
                        }

                        // AnkiWeb은 페이지를 새로 불러오지 않고 주소만 바꾸므로 여기서도 덱 페이지인지 다시 본다.
                        override fun doUpdateVisitedHistory(
                            view: WebView,
                            url: String?,
                            isReload: Boolean
                        ) {
                            canGoBack = view.canGoBack()
                            onDeckPage = isDeckPage(url)
                        }
                    }
                webChromeClient =
                    object : WebChromeClient() {
                        override fun onProgressChanged(
                            view: WebView,
                            newProgress: Int
                        ) {
                            progress = newProgress
                        }
                    }
                setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                    currentOnDownload(url, userAgent, contentDisposition, mimeType)
                }
                loadUrl(SHARED_DECKS_URL)
            }
        }
    DisposableEffect(webView) {
        onDispose { webView.destroy() }
    }
    BackHandler {
        if (canGoBack) webView.goBack() else onClose()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AnkiWeb 공유 덱") },
                subtitle = { Text("다운로드만 받으면 바로 가져와요") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(painter = painterResource(R.drawable.ic_close), contentDescription = "닫기")
                    }
                }
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = onDeckPage,
                enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeIn(),
                exit = shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeOut()
            ) {
                ImportBar(onClick = { webView.evaluateJavascript(CLICK_DOWNLOAD_SCRIPT, null) })
            }
        }
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
        ) {
            AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())
            // 페이지가 밀리지 않도록 진행 표시는 페이지 위에 겹친다.
            AnimatedVisibility(
                visible = progress < 100,
                enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
                exit = fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                LinearWavyProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun isDeckPage(url: String?): Boolean {
    val uri = url?.toUri() ?: return false
    return uri.host?.endsWith("ankiweb.net") == true && DECK_PAGE_PATH.matches(uri.path.orEmpty())
}

/** 덱 페이지 아래에 붙는 가져오기 버튼. 학습하기 버튼과 같은 크기로 이 화면의 주인공이 된다. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ImportBar(onClick: () -> Unit) {
    val height = 64.dp
    Box(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Button(
            onClick = onClick,
            shapes = ButtonDefaults.shapesFor(height),
            contentPadding = ButtonDefaults.contentPaddingFor(height),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = height)
        ) {
            Text(
                text = "이 단어장 가져오기",
                style = MaterialTheme.typography.headlineSmallEmphasized,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

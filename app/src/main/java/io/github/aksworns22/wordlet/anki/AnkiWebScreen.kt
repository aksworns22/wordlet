package io.github.aksworns22.wordlet.anki

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
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

/** 덱 페이지의 Download가 이동하는 경로. */
private val DECK_DOWNLOAD_PATH = Regex("^/svc/shared/download-deck/\\d+$")

/** AnkiWeb이 페이지 제목 뒤에 붙이는 꼬리. */
private const val TITLE_SUFFIX = " - AnkiWeb"

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
 * 덱 페이지에서 Download를 누르면 [onDownload]로 덱 주소와 이름을 넘겨 앱이 직접 받는다. AnkiWeb 밖의 링크는 브라우저로 연다.
 * 덱 페이지에서는 Download를 찾지 않아도 되도록 페이지 위에 가져오기 버튼을 띄운다.
 */
@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AnkiWebScreen(
    onDownload: (url: String, userAgent: String, deckName: String?) -> Unit,
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
                            // WebView가 받기 시작하면 앱이 다시 받아 요청이 두 번 가므로, 보내기 전에 가로채 앱이 한 번만 받는다.
                            if (isDeckDownload(request.url)) {
                                currentOnDownload(
                                    request.url.toString(),
                                    view.settings.userAgentString,
                                    view.title?.removeSuffix(TITLE_SUFFIX)?.takeIf { it.isNotBlank() }
                                )
                                return true
                            }
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
                // 가로채지 못한 다운로드는 파일 이름을 덱 이름으로 쓴다.
                setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                    val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
                    currentOnDownload(url, userAgent, fileName.substringBeforeLast('.'))
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
            AnimatedVisibility(
                visible = onDeckPage,
                enter = slideInVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) { it } + fadeIn(),
                exit = slideOutVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                ImportButton(onClick = { webView.evaluateJavascript(CLICK_DOWNLOAD_SCRIPT, null) })
            }
        }
    }
}

private fun isDeckDownload(uri: Uri): Boolean = uri.host?.endsWith("ankiweb.net") == true && DECK_DOWNLOAD_PATH.matches(uri.path.orEmpty())

private fun isDeckPage(url: String?): Boolean {
    val uri = url?.toUri() ?: return false
    return uri.host?.endsWith("ankiweb.net") == true && DECK_PAGE_PATH.matches(uri.path.orEmpty())
}

/** 덱 페이지 위에 떠 있는 가져오기 버튼. 학습하기 버튼과 같은 크기로 이 화면의 주인공이 된다. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ImportButton(onClick: () -> Unit) {
    val height = 64.dp
    Box(
        Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Button(
            onClick = onClick,
            shapes = ButtonDefaults.shapesFor(height),
            contentPadding = ButtonDefaults.contentPaddingFor(height),
            // 페이지 위에서 떠 보이도록 FAB만큼 그림자를 준다.
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp, pressedElevation = 6.dp),
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

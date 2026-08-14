package com.bdf.saleor.ui.checkout

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import com.bdf.saleor.feature.checkout.R

@Composable
fun KakaoPostcodeDialog(
    onResult: (KakaoAddressPatch) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnResult = rememberUpdatedState(onResult)
    BackHandler(onBack = onDismiss)
    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("checkout_postcode_dialog"),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.End)
                    .testTag("checkout_postcode_close"),
            ) {
                Text(stringResource(R.string.checkout_close))
            }
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                factory = { context ->
                    createKakaoPostcodeWebView(context) { selection ->
                        currentOnResult.value(selection.toAddressPatch())
                    }
                },
                onRelease = { webView ->
                    webView.removeJavascriptInterface(KAKAO_POSTCODE_BRIDGE)
                    webView.destroy()
                },
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createKakaoPostcodeWebView(
    context: Context,
    onComplete: (KakaoPostcodeSelection) -> Unit,
): WebView {
    val mainHandler = Handler(Looper.getMainLooper())
    val assetLoader = WebViewAssetLoader.Builder()
        .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
        .build()
    return WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
        webChromeClient = WebChromeClient()
        webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest,
            ) = if (request.url.host == "appassets.androidplatform.net") {
                assetLoader.shouldInterceptRequest(request.url)
            } else {
                null
            }
        }
        addJavascriptInterface(
            KakaoPostcodeBridge(mainHandler, onComplete),
            KAKAO_POSTCODE_BRIDGE,
        )
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (width <= 0 || height <= 0) return
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                loadUrl(KAKAO_POSTCODE_URL)
            }
        })
    }
}

private class KakaoPostcodeBridge(
    private val mainHandler: Handler,
    private val onComplete: (KakaoPostcodeSelection) -> Unit,
) {
    @JavascriptInterface
    fun onComplete(
        zonecode: String,
        address: String,
        roadAddress: String,
        jibunAddress: String,
        userSelectedType: String,
        sido: String,
        sigungu: String,
        bname: String,
    ) {
        val selection = KakaoPostcodeSelection(
            zonecode = zonecode,
            address = address,
            roadAddress = roadAddress,
            jibunAddress = jibunAddress,
            userSelectedType = userSelectedType,
            sido = sido,
            sigungu = sigungu,
            bname = bname,
        )
        mainHandler.post { onComplete(selection) }
    }
}

private const val KAKAO_POSTCODE_BRIDGE = "KakaoPostcodeBridge"
private const val KAKAO_POSTCODE_URL =
    "https://appassets.androidplatform.net/assets/kakao_postcode.html"

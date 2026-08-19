package com.bdf.saleor.feature.checkout.checkout

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
import com.bdf.saleor.core.model.KakaoAddressPatch
import com.bdf.saleor.core.model.KakaoPostcodeSelection
import com.bdf.saleor.core.model.toAddressPatch
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import com.bdf.saleor.feature.checkout.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KakaoPostcodeDialog(
    onResult: (KakaoAddressPatch) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnResult = rememberUpdatedState(onResult)
    BackHandler(onBack = onDismiss)
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("checkout_postcode_dialog"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.checkout_find_address)) },
                navigationIcon = {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("checkout_postcode_close"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.checkout_close),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        AndroidView(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth()
                .fillMaxSize(),
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

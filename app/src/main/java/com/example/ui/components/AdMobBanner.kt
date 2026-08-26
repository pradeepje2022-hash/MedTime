package com.example.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.BuildConfig
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

// User's configured AdMob IDs
const val ADMOB_APP_ID = "ca-app-pub-2684605699147771~6067113228"
const val ADMOB_BANNER_AD_UNIT_ID = "ca-app-pub-2684605699147771/4690452372"
// Google's official sample banner test ad unit ID
const val ADMOB_TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"

private const val TAG = "AdMobBanner"

@Composable
fun AdMobBanner(
    modifier: Modifier = Modifier,
    adUnitId: String = ADMOB_BANNER_AD_UNIT_ID
) {
    val isInPreview = LocalInspectionMode.current
    if (isInPreview) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "AdMob Banner Preview",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Using user's configured Ad Unit ID directly
    val targetAdUnitId = adUnitId

    var isAdLoaded by remember { mutableStateOf(false) }
    var adViewInstance by remember { mutableStateOf<AdView?>(null) }

    DisposableEffect(lifecycleOwner, adViewInstance) {
        val observer = LifecycleEventObserver { _, event ->
            val view = adViewInstance ?: return@LifecycleEventObserver
            try {
                when (event) {
                    Lifecycle.Event.ON_RESUME -> view.resume()
                    Lifecycle.Event.ON_PAUSE -> view.pause()
                    Lifecycle.Event.ON_DESTROY -> view.destroy()
                    else -> {}
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error managing AdView lifecycle event: $event", e)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            try {
                adViewInstance?.destroy()
            } catch (e: Exception) {
                Log.e(TAG, "Error destroying AdView", e)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("admob_banner_container"),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(AdSize.BANNER)
                    setAdUnitId(targetAdUnitId)
                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            super.onAdLoaded()
                            isAdLoaded = true
                            Log.d(TAG, "Ad successfully loaded with ID: $targetAdUnitId")
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            super.onAdFailedToLoad(error)
                            isAdLoaded = false
                            Log.w(TAG, "Ad failed to load (code ${error.code}): ${error.message}")
                        }
                    }
                    try {
                        loadAd(AdRequest.Builder().build())
                    } catch (e: Exception) {
                        Log.e(TAG, "Exception while requesting ad", e)
                    }
                    adViewInstance = this
                }
            },
            modifier = Modifier.testTag("admob_banner_view")
        )
    }
}

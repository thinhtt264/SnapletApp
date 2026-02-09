package com.thinh.snaplet

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.thinh.snaplet.ui.app.AppViewModel
import com.thinh.snaplet.ui.screens.MainScreen
import com.thinh.snaplet.utils.Logger
import com.thinh.snaplet.utils.deeplink.DeepLinkEvent
import com.thinh.snaplet.utils.deeplink.DeepLinkManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val appViewModel: AppViewModel by viewModels()

    @Inject
    lateinit var deepLinkManager: DeepLinkManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().apply {
            setKeepOnScreenCondition { appViewModel.uiState.value.isLoading }
        }

        super.onCreate(savedInstanceState)

        handleDeepLink(intent)

        enableEdgeToEdge()
        setContent { MainScreen(appViewModel) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        setIntent(intent)

        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val data = intent.data

            if (data != null) {
                Logger.d("🔗 DeepLink received: $data")

                // Extract userName parameter
                val userName = data.getQueryParameter("userName")

                // Emit event if userName is present
                if (!userName.isNullOrBlank()) {
                    Logger.d("👤 Emitting friend request event for: $userName")

                    // Launch coroutine to emit event
                    lifecycleScope.launch {
                        deepLinkManager.emitEvent(
                            DeepLinkEvent.FriendRequest(userName)
                        )
                    }
                } else {
                    Logger.w("⚠️ DeepLink missing userName parameter")
                }
            }
        }
    }
}
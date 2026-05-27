package com.legalmotion

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.legalmotion.ui.CourtroomDictionaryScreen
import com.legalmotion.ui.LegalMotionScreen
import com.legalmotion.ui.theme.LegalMotionTheme
import com.google.android.gms.ads.MobileAds

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val launchTime = SystemClock.uptimeMillis()
        installSplashScreen().setKeepOnScreenCondition {
            SystemClock.uptimeMillis() - launchTime < 1000
        }
        super.onCreate(savedInstanceState)
        
        // Initialize Google Mobile Ads SDK
        MobileAds.initialize(this) {}

        setContent {
            var darkModeEnabled by rememberSaveable { mutableStateOf(false) }
            var showDictionary by rememberSaveable { mutableStateOf(false) }

            LegalMotionTheme(darkTheme = darkModeEnabled) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (showDictionary) {
                        BackHandler { showDictionary = false }
                        CourtroomDictionaryScreen(onBack = { showDictionary = false })
                    } else {
                        LegalMotionScreen(
                            darkModeEnabled = darkModeEnabled,
                            onDarkModeChange = { darkModeEnabled = it },
                            onOpenDictionary = { showDictionary = true }
                        )
                    }
                }
            }
        }
    }
}

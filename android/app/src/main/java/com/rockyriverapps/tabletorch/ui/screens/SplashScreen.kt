package com.rockyriverapps.tabletorch.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.rockyriverapps.tabletorch.R
import com.rockyriverapps.tabletorch.ui.theme.CinzelFont
import com.rockyriverapps.tabletorch.ui.theme.TableTorchTheme
import com.rockyriverapps.tabletorch.ui.theme.TorchOrange
import kotlinx.coroutines.delay

/**
 * Splash screen displayed for 1.4 seconds on app launch.
 * Shows the app logo and title with fade animation.
 */
@Composable
fun SplashScreen(
    onTimeout: () -> Unit
) {
    var startFadeOut by remember { mutableStateOf(false) }

    // Fade out animation
    val alpha by animateFloatAsState(
        targetValue = if (startFadeOut) 0f else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "splash_fade"
    )

    // Timer for splash duration (1.4 seconds like iOS)
    LaunchedEffect(Unit) {
        delay(1100) // Show for 1.1 seconds before starting fade
        startFadeOut = true
        delay(300) // Wait for fade animation
        onTimeout()
    }

    val tapToSkipDescription = stringResource(R.string.tap_to_skip)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .alpha(alpha)
            .semantics {
                contentDescription = tapToSkipDescription
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onTimeout()
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Flame icon
            Image(
                painter = painterResource(id = R.drawable.ic_flame_filled),
                contentDescription = stringResource(R.string.splash_logo_description),
                modifier = Modifier.size(160.dp),
                colorFilter = ColorFilter.tint(TorchOrange)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // App title
            Text(
                text = stringResource(R.string.splash_title),
                fontFamily = CinzelFont,
                fontWeight = FontWeight.Bold,
                fontSize = 34.sp,
                letterSpacing = 0.8.sp,
                color = Color.White
            )
        }
    }
}

// ============================================================================
// Previews
// ============================================================================

@Preview(showBackground = true)
@Composable
private fun SplashScreenPreview() {
    TableTorchTheme {
        SplashScreen(onTimeout = {})
    }
}

package com.example.plannerapp.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plannerapp.theme.AppBorderLight
import com.example.plannerapp.theme.AppDimens
import com.example.plannerapp.theme.AppTextPrimaryLight

// Google brand mark colors
private val GoogleRed    = Color(0xFFEA4335)
private val GoogleBlue   = Color(0xFF4285F4)
private val GoogleYellow = Color(0xFFFBBC05)
private val GoogleGreen  = Color(0xFF34A853)

@Composable
fun SocialAuthSection(
    isLoading: Boolean,
    onGoogleSignInClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val googleSource = remember { MutableInteractionSource() }

        // Google Sign-In button: white background, 1dp border #E2E5EA, height 54dp, cornerRadius 14dp
        Surface(
            onClick = onGoogleSignInClick,
            enabled = !isLoading,
            interactionSource = googleSource,
            modifier = Modifier
                .fillMaxWidth()
                .height(AppDimens.ButtonHeight),
            shape = RoundedCornerShape(AppDimens.CornerButton),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = AppDimens.Space16),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Minimalist Google G
                val googleG = buildAnnotatedString {
                    withStyle(SpanStyle(color = GoogleBlue, fontWeight = FontWeight.Bold, fontSize = 18.sp)) { append("G") }
                }
                Text(text = googleG)

                Spacer(modifier = Modifier.width(AppDimens.Space12))

                Text(
                    text = "Continue with Google",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

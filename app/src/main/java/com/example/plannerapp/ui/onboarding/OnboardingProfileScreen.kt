package com.example.plannerapp.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plannerapp.theme.AppDimens
import com.example.plannerapp.theme.AppDisabledBgLight
import com.example.plannerapp.theme.AppDisabledText
import com.example.plannerapp.theme.AppFocusBorderDark
import com.example.plannerapp.theme.AppPrimary
import com.example.plannerapp.theme.AppPrimaryButton
import com.example.plannerapp.theme.AppPrimaryButtonPressed
import com.example.plannerapp.theme.AppSuccess

@Composable
fun OnboardingProfileScreen(
    displayName: String,
    onDisplayNameChange: (String) -> Unit,
    username: String,
    onUsernameChange: (String) -> Unit,
    usernameValidationState: UsernameValidationState,
    usernameErrorMessage: String?,
    avatarUri: android.net.Uri? = null,
    onAvatarSelected: (android.net.Uri) -> Unit = {},
    onContinue: () -> Unit
) {
    val cameraInteractionSource = remember { MutableInteractionSource() }
    val isDark = isSystemInDarkTheme()
    val focusBorderColor = if (isDark) AppFocusBorderDark else AppPrimary

    val photoPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { onAvatarSelected(it) }
    }

    val isUsernameValid = username.isBlank() || usernameValidationState == UsernameValidationState.VALID
    val isCtaEnabled = displayName.isNotBlank() && isUsernameValid && usernameValidationState != UsernameValidationState.CHECKING

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AppDimens.ScreenHorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(AppDimens.Space16))

        Text(
            text = "Create your profile",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "This is how people will recognize you on Tusknet.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(AppDimens.SectionVerticalGap))

        // ── Avatar Upload Section (88dp diameter, background #D4720E, 32sp bold initials, 28dp camera badge) ──
        Box(
            modifier = Modifier.size(AppDimens.AvatarLg),
            contentAlignment = Alignment.BottomEnd
        ) {
            // Main Avatar Circle: 88dp, background #D4720E
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(AppPrimary),
                contentAlignment = Alignment.Center
            ) {
                if (avatarUri != null) {
                    coil.compose.AsyncImage(
                        model = avatarUri,
                        contentDescription = "Profile Avatar",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    val initial = displayName.firstOrNull()?.uppercase() ?: "A"
                    Text(
                        text = initial,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Camera Badge: 28dp circle, #FFFFFF, soft shadow, icon #D4720E
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 3.dp,
                modifier = Modifier
                    .size(AppDimens.IconBoxSm)
                    .clickable(
                        interactionSource = cameraInteractionSource,
                        indication = null,
                        onClick = {
                            photoPickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        }
                    )
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CameraAlt,
                        contentDescription = "Upload Avatar",
                        modifier = Modifier.size(16.dp),
                        tint = AppPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(AppDimens.SectionVerticalGap))

        // ── Display Name Field: 54dp, #F0F2F5, 1.5dp focus border (#D4720E), 12dp radius ──
        OutlinedTextField(
            value = displayName,
            onValueChange = onDisplayNameChange,
            placeholder = { Text("Display name", style = MaterialTheme.typography.bodyLarge, color = AppDisabledText) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.PersonOutline,
                    contentDescription = null,
                    modifier = Modifier.size(AppDimens.IconSizeMd),
                    tint = AppDisabledText
                )
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(AppDimens.InputHeight),
            shape = RoundedCornerShape(AppDimens.CornerCompact),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedBorderColor = focusBorderColor,
                unfocusedBorderColor = Color.Transparent,
                errorBorderColor = MaterialTheme.colorScheme.error
            )
        )

        Spacer(modifier = Modifier.height(AppDimens.Space16))

        // ── Username Field with Validation ──
        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            placeholder = { Text("Username (optional)", style = MaterialTheme.typography.bodyLarge, color = AppDisabledText) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.AlternateEmail,
                    contentDescription = null,
                    modifier = Modifier.size(AppDimens.IconSizeMd),
                    tint = AppDisabledText
                )
            },
            trailingIcon = {
                when (usernameValidationState) {
                    UsernameValidationState.CHECKING -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = AppPrimary
                        )
                    }
                    UsernameValidationState.VALID -> {
                        if (username.isNotBlank()) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Available",
                                tint = AppSuccess,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    UsernameValidationState.INVALID -> {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = "Invalid",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    UsernameValidationState.IDLE -> {}
                }
            },
            isError = usernameValidationState == UsernameValidationState.INVALID,
            supportingText = {
                if (usernameValidationState == UsernameValidationState.INVALID && usernameErrorMessage != null) {
                    Text(
                        text = usernameErrorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (usernameValidationState == UsernameValidationState.VALID && username.isNotBlank()) {
                    Text(
                        text = "Username is available",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppSuccess
                    )
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(if (usernameValidationState == UsernameValidationState.INVALID || (usernameValidationState == UsernameValidationState.VALID && username.isNotBlank())) 74.dp else AppDimens.InputHeight),
            shape = RoundedCornerShape(AppDimens.CornerCompact),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedBorderColor = focusBorderColor,
                unfocusedBorderColor = Color.Transparent,
                errorBorderColor = MaterialTheme.colorScheme.error
            )
        )

        Spacer(modifier = Modifier.weight(1f))

        // ── Primary Button: #B85E08 (accessible surface), darkens on press to #9E4F04, white text ──
        val buttonSource = remember { MutableInteractionSource() }
        val isButtonPressed by buttonSource.collectIsPressedAsState()
        val buttonBg = if (isButtonPressed) AppPrimaryButtonPressed else AppPrimaryButton

        Button(
            onClick = onContinue,
            enabled = isCtaEnabled,
            interactionSource = buttonSource,
            modifier = Modifier
                .fillMaxWidth()
                .height(AppDimens.ButtonHeight),
            shape = RoundedCornerShape(AppDimens.CornerButton),
            colors = ButtonDefaults.buttonColors(
                containerColor = buttonBg,
                contentColor = Color.White,
                disabledContainerColor = AppDisabledBgLight,
                disabledContentColor = AppDisabledText
            )
        ) {
            Text(
                text = "Create my profile",
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(modifier = Modifier.height(AppDimens.Space28))
    }
}

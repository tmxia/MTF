package com.aazhou.mtforum.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aazhou.mtforum.R
import com.aazhou.mtforum.ui.theme.LocalIsDarkTheme
import com.aazhou.mtforum.ui.theme.LocalMtfColors
import com.aazhou.mtforum.ui.theme.bodyTextColor

@Composable
fun BlockUserDialog(
    targetName: String,
    isBlock: Boolean,
    isSubmitting: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val colors = LocalMtfColors.current
    val isDark = LocalIsDarkTheme.current
    
    
    val actionColor = if (isBlock) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary

    Dialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .then(
                    if (isDark) {
                        Modifier.border(0.5.dp, colors.divider, RoundedCornerShape(22.dp))
                    } else {
                        Modifier
                    },
                ),
            shape = RoundedCornerShape(22.dp),
            color = colors.surface,
            tonalElevation = 0.dp,
            shadowElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(actionColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_forbid),
                        contentDescription = null,
                        tint = actionColor,
                        modifier = Modifier.size(26.dp),
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = if (isBlock) "加入黑名单" else "移除黑名单",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                    fontWeight = FontWeight.Bold,
                    color = bodyTextColor(),
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = if (isBlock) 
                        "确定将「$targetName」加入黑名单吗？\n加入后对方将无法与您互动。" 
                    else 
                        "确定将「$targetName」移出黑名单吗？\n移除后，你们将恢复正常互动。",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                )
                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.quoteBackground)
                            .clickable(enabled = !isSubmitting, onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "取消",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            fontWeight = FontWeight.Medium,
                            color = bodyTextColor(),
                        )
                    }

                    val btnBg = if (isDark) actionColor.copy(alpha = 0.22f) else actionColor
                    val btnTextColor = if (isDark) actionColor else Color.White
                    Button(
                        onClick = onConfirm,
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .then(
                                if (isDark) {
                                    Modifier.border(1.dp, actionColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                } else {
                                    Modifier
                                },
                            ),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = btnBg,
                            contentColor = btnTextColor,
                            disabledContainerColor = actionColor.copy(alpha = 0.5f),
                            disabledContentColor = btnTextColor.copy(alpha = 0.8f),
                        ),
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = btnTextColor,
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            text = if (isBlock) "确定拉黑" else "解除拉黑",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
        }
    }
}

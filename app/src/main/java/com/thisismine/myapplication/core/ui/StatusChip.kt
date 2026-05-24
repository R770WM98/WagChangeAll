package com.thisismine.myapplication.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
// ...existing code...
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.thisismine.myapplication.ui.theme.MyApplicationTheme
import com.thisismine.myapplication.ui.theme.StatusDueSoonDark
import com.thisismine.myapplication.ui.theme.StatusDueSoonLight
import com.thisismine.myapplication.ui.theme.StatusInfoDark
import com.thisismine.myapplication.ui.theme.StatusInfoLight
import com.thisismine.myapplication.ui.theme.StatusOnTrackDark
import com.thisismine.myapplication.ui.theme.StatusOnTrackLight
import com.thisismine.myapplication.ui.theme.StatusOverdueDark
import com.thisismine.myapplication.ui.theme.StatusOverdueLight

enum class StatusTone {
    Critical,
    Warning,
    Info,
    Positive,
    Neutral
}

@Composable
fun StatusChip(
    text: String,
    tone: StatusTone,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val motion = LocalMotionScheme.current
    val targetColor = tone.toColor(isDark)
    val color = animateColorAsState(
        targetValue = targetColor,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = motion.baseMillis),
        label = "status-chip-color"
    ).value
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.18f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
            Text(
                text = text,
                color = color,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

private fun StatusTone.toColor(isDark: Boolean): Color = when (this) {
    StatusTone.Critical -> if (isDark) StatusOverdueDark else StatusOverdueLight
    StatusTone.Warning -> if (isDark) StatusDueSoonDark else StatusDueSoonLight
    StatusTone.Info -> if (isDark) StatusInfoDark else StatusInfoLight
    StatusTone.Positive -> if (isDark) StatusOnTrackDark else StatusOnTrackLight
    StatusTone.Neutral -> if (isDark) StatusInfoDark.copy(alpha = 0.8f) else StatusInfoLight.copy(alpha = 0.85f)
}

@Preview(name = "Status Chip - Tones", showBackground = true)
@Composable
private fun StatusChipPreview() {
    MyApplicationTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip(text = "Critical", tone = StatusTone.Critical, icon = Icons.Filled.Warning)
            StatusChip(text = "Warning", tone = StatusTone.Warning)
            StatusChip(text = "Info", tone = StatusTone.Info, icon = Icons.Filled.Info)
            StatusChip(text = "Positive", tone = StatusTone.Positive)
        }
    }
}

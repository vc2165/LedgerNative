@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.vinay.ledger.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** White rounded card used for every section. */
@Composable
fun SectionCard(modifier: Modifier = Modifier, title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 0.dp,
        shadowElevation = 1.dp,
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            if (title != null) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            }
            content()
        }
    }
}

/** A tappable two-line row with optional trailing content, like a settings item. */
@Composable
fun NavRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    titleColor: Color = Color.Unspecified,
    trailing: (@Composable () -> Unit)? = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.outline) },
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(16.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = titleColor)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (trailing != null) { Spacer(Modifier.width(8.dp)); trailing() }
    }
}

/** Label / value line used in reports. */
@Composable
fun KvRow(
    label: String,
    value: String,
    bold: Boolean = false,
    valueColor: Color = Color.Unspecified,
    dot: Color? = null,
    sub: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick) else Modifier)
            .padding(vertical = 9.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot != null) { Box(Modifier.size(9.dp).clip(CircleShape).background(dot)); Spacer(Modifier.width(10.dp)) }
        Text(label, Modifier.weight(1f), fontWeight = if (bold) FontWeight.SemiBold else null, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Medium, color = valueColor)
        if (sub != null) { Spacer(Modifier.width(6.dp)); Text(sub, color = MaterialTheme.colorScheme.outline, fontSize = 13.sp) }
        if (onClick != null) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
    }
}

/** Category icon in a tinted rounded square. */
@Composable
fun CatAvatar(icon: ImageVector, color: Color, size: Int = 42) {
    Box(
        Modifier.size(size.dp).clip(RoundedCornerShape((size * 0.33f).dp)).background(color.copy(alpha = 0.13f)),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = color, modifier = Modifier.size((size * 0.52f).dp)) }
}

/** Budget meter that fills in when it first appears. */
@Composable
fun Meter(label: String, used: String, fraction: Float?, of: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(used, style = MaterialTheme.typography.titleMedium)
        if (fraction != null) {
            val anim = remember { Animatable(0f) }
            LaunchedEffect(fraction) { anim.animateTo(fraction.coerceIn(0f, 1f), tween(700, easing = EaseStd)) }
            val col = when { fraction >= 1f -> Bad; fraction >= 0.8f -> Warn; else -> Purple }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { anim.value }, color = col, trackColor = Color(0xFFECE9F5), strokeCap = StrokeCap.Round,
                modifier = Modifier.fillMaxWidth().height(6.dp), gapSize = 0.dp, drawStopIndicator = {},
            )
            Spacer(Modifier.height(4.dp))
        }
        Text(of, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
    }
}

/** Top bar for sub-screens: back arrow + title + actions. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubBar(title: String, onBack: () -> Unit, scroll: TopAppBarScrollBehavior? = null, actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
        actions = actions,
        scrollBehavior = scroll,
    )
}

@Composable
fun Muted(text: String, modifier: Modifier = Modifier) =
    Text(text, modifier = modifier, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
fun FieldLabel(text: String) =
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))

val ScreenPadding = PaddingValues(bottom = 24.dp)
val RowGap = Arrangement.spacedBy(8.dp)

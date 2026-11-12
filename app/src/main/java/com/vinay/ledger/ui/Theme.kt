@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.vinay.ledger.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.DeliveryDining
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.LocalLaundryService
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinay.ledger.data.LedgerData
import com.vinay.ledger.data.cat
import com.vinay.ledger.data.resolve

val Purple = Color(0xFF5B2FC4)
val PurpleDeep = Color(0xFF3F1F8C)
val PurpleTint = Color(0xFFEFE8FF)
val Good = Color(0xFF16935A)
val Bad = Color(0xFFD92D3A)
val Warn = Color(0xFFD98A00)
val CFixed = Color(0xFF9A96AD)
val COneoff = Color(0xFFE0952B)

private val scheme = lightColorScheme(
    primary = Purple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6DEFC),
    onPrimaryContainer = Color(0xFF22005D),
    secondary = Color(0xFF625B71),
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF7D5260),
    background = Color(0xFFF7F5FB),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFF7F5FB),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF5F5B68),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF1EDF8),
    surfaceContainerHigh = Color(0xFFEBE6F4),
    surfaceContainerHighest = Color(0xFFE5DFEF),
    outline = Color(0xFF7A7585),
    outlineVariant = Color(0xFFDDD7E6),
    error = Bad,
)

private val type = Typography().let { t ->
    t.copy(
        headlineLarge = t.headlineLarge.copy(fontWeight = FontWeight.SemiBold),
        headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun LedgerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = scheme,
        typography = type,
        shapes = Shapes(
            small = RoundedCornerShape(10.dp), medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(22.dp),
        ),
        content = content,
    )
}

val BigAmount = TextStyle(fontSize = 46.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, letterSpacing = (-0.5).sp)

private val ICON_BY_ID: Map<String, ImageVector> = mapOf(
    "rent" to Icons.Outlined.Home, "physio_bill" to Icons.Outlined.AccessibilityNew, "physio_consult" to Icons.Outlined.AccessibilityNew,
    "swiggy" to Icons.Outlined.DeliveryDining, "zomato" to Icons.Outlined.DeliveryDining, "lunch" to Icons.Outlined.Restaurant,
    "eatout" to Icons.Outlined.Restaurant, "snacks" to Icons.Outlined.Spa, "grocery" to Icons.Outlined.ShoppingCart,
    "t_office" to Icons.Outlined.DirectionsCar, "t_physio" to Icons.Outlined.DirectionsCar, "t_local" to Icons.Outlined.DirectionsBus,
    "electricity" to Icons.Outlined.Bolt, "laundry" to Icons.Outlined.LocalLaundryService, "recharge" to Icons.Outlined.PhoneAndroid,
    "sub_rec" to Icons.Outlined.Subscriptions, "sub_one" to Icons.Outlined.Subscriptions, "salon" to Icons.Outlined.ContentCut,
    "travel" to Icons.Outlined.Flight, "gifts" to Icons.Outlined.CardGiftcard, "entertainment" to Icons.Outlined.Movie,
    "personal" to Icons.Outlined.ShoppingBag, "health" to Icons.Outlined.LocalHospital, "other" to Icons.Outlined.MoreHoriz,
)
private val ICON_WORDS: List<Pair<Regex, ImageVector>> = listOf(
    "rent|house|home|pg|flat" to Icons.Outlined.Home,
    "swiggy|zomato|deliver" to Icons.Outlined.DeliveryDining,
    "lunch|dinner|food|eat|restaurant|meal" to Icons.Outlined.Restaurant,
    "tea|coffee|cafe|chai" to Icons.Outlined.LocalCafe,
    "fruit|snack" to Icons.Outlined.Spa,
    "grocer|blinkit|zepto|bigbasket|mart" to Icons.Outlined.ShoppingCart,
    "cab|uber|ola|car|fuel|petrol|commute|auto" to Icons.Outlined.DirectionsCar,
    "bus|metro|train|ride|transport" to Icons.Outlined.DirectionsBus,
    "electric|power|bill" to Icons.Outlined.Bolt,
    "laundry|cloth|wash" to Icons.Outlined.LocalLaundryService,
    "recharge|mobile|phone|internet|wifi" to Icons.Outlined.PhoneAndroid,
    "subscri|netflix|prime|ott|spotify" to Icons.Outlined.Subscriptions,
    "salon|hair|groom" to Icons.Outlined.ContentCut,
    "travel|trip|flight|hotel" to Icons.Outlined.Flight,
    "gift|family|donation" to Icons.Outlined.CardGiftcard,
    "movie|entertain|game|fun" to Icons.Outlined.Movie,
    "medic|health|doctor|pharma|hospital" to Icons.Outlined.LocalHospital,
    "physio|gym|fitness" to Icons.Outlined.AccessibilityNew,
    "shop|amazon|flipkart|fee" to Icons.Outlined.ShoppingBag,
).map { Regex(it.first) to it.second }

fun LedgerData.catIcon(id: String): ImageVector {
    val rid = resolve(id)
    ICON_BY_ID[rid]?.let { return it }
    val n = cat(id).name.lowercase()
    return ICON_WORDS.firstOrNull { it.first.containsMatchIn(n) }?.second ?: Icons.Outlined.MoreHoriz
}

fun LedgerData.catColor(id: String) = Color(cat(id).color)

package com.khanearam.shop

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.abs

// ---------- اجزای مشترک ظاهر (دکمه‌ها، اسلایدرها، هدرها) ----------

/** دکمه اصلی: گرادیانی، گرد، با افکت فشردن. جایگزین Button. */
@Composable
fun FancyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && enabled) 0.96f else 1f, label = "fancyBtn")
    val shape = RoundedCornerShape(26.dp)
    val bg: Brush = if (enabled) Brand.headerGradient else Brush.linearGradient(
        listOf(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
    )
    val fg = if (enabled) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(if (enabled) 6.dp else 0.dp, shape, ambientColor = Brand.violet, spotColor = Brand.violet)
            .clip(shape)
            .background(bg)
            .clickable(interactionSource = src, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 22.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides fg) {
            ProvideTextStyle(MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, content = content)
            }
        }
    }
}

/** دکمه دوم: لبه‌دار با زمینه بنفش ملایم. جایگزین OutlinedButton. */
@Composable
fun SoftButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && enabled) 0.97f else 1f, label = "softBtn")
    val shape = RoundedCornerShape(24.dp)
    val main = MaterialTheme.colorScheme.primary
    val fg = if (enabled) main else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    val border = if (enabled) main.copy(alpha = 0.55f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(if (pressed && enabled) main.copy(alpha = 0.16f) else main.copy(alpha = 0.07f))
            .border(1.5.dp, border, shape)
            .clickable(interactionSource = src, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .heightIn(min = 46.dp)
            .padding(horizontal = 20.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides fg) {
            ProvideTextStyle(MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, content = content)
            }
        }
    }
}

/** نقطه‌های راهنمای اسلایدر */
@Composable
fun DotsRow(count: Int, current: Int, modifier: Modifier = Modifier) {
    if (count < 2) return
    Row(
        modifier.fillMaxWidth().padding(top = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until count) {
            val on = current == i
            val w by animateFloatAsState(if (on) 20f else 7f, label = "dot")
            Box(
                Modifier.padding(horizontal = 3.dp).height(7.dp).width(w.dp)
                    .background(if (on) Brand.pink else Brand.lavender, RoundedCornerShape(4.dp))
            )
        }
    }
}

/** کوچک‌شدن صفحه‌های کناری اسلایدر برای حس عمق */
fun Modifier.pageScale(pager: PagerState, page: Int): Modifier = this.graphicsLayer {
    val off = abs((pager.currentPage - page) + pager.currentPageOffsetFraction).coerceIn(0f, 1f)
    val s = 1f - 0.07f * off
    scaleX = s
    scaleY = s
    alpha = 1f - 0.25f * off
}

/** عنوان بخش با خط رنگی و «مشاهده همه» */
@Composable
fun SectionHeader(title: String, onAll: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(16.dp, 20.dp, 8.dp, 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(4.dp).height(20.dp).background(Brand.pink, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(8.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f))
        if (onAll != null) TextButton(onClick = onAll) { Text("مشاهده همه") }
    }
}

data class Promo(val emoji: String, val title: String, val sub: String, val brush: Brush, val route: String)

object Promos {
    val shop = listOf(
        Promo("🎀", "تخفیف‌های ویژه", "قیمت‌های خاص را از دست ندهید", Brand.promo2, "list/0"),
        Promo("🎡", "گردونه شانس", "هر روز یک شانس برای برنده شدن", Brand.promo3, "wheel"),
        Promo("✨", "تازه‌های فروشگاه", "جدیدترین محصولات خانه آرام", Brand.promo1, "list/0")
    )
    val club = listOf(
        Promo("🎁", "باشگاه مشتریان", "با هر خرید امتیاز بگیرید", Brand.promo1, "wheel"),
        Promo("👛", "کیف پول و کارت هدیه", "شارژ آنلاین و هدیه دادن", Brand.promo2, "wallet"),
        Promo("✅", "اصالت کالا", "کد روی بسته را بررسی کنید", Brand.promo3, "verify")
    )
    val account = listOf(
        Promo("🎡", "گردونه شانس", "امروز شانستان را امتحان کنید", Brand.promo3, "wheel"),
        Promo("📖", "مقالات آموزشی", "بخوانید و بیشتر بدانید", Brand.promo1, "articles"),
        Promo("💬", "پشتیبانی", "هر سؤالی دارید بپرسید", Brand.promo2, "support")
    )
}

/** اسلایدر خودکار بنرهای گرادیانی برای صفحه‌های داخلی */
@Composable
fun PromoPager(promos: List<Promo>, nav: NavController, modifier: Modifier = Modifier, height: Dp = 108.dp, edge: Dp = 16.dp) {
    val pager = rememberPagerState(pageCount = { promos.size })
    LaunchedEffect(promos.size) {
        if (promos.size > 1) {
            while (true) {
                delay(4200)
                if (!pager.isScrollInProgress) {
                    pager.animateScrollToPage((pager.currentPage + 1) % promos.size)
                }
            }
        }
    }
    Column(modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pager,
            contentPadding = PaddingValues(horizontal = edge),
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) { i ->
            val s = promos[i]
            Box(
                Modifier.fillMaxWidth().height(height).pageScale(pager, i)
                    .clip(RoundedCornerShape(20.dp)).background(s.brush)
                    .clickable {
                        Store.query = ""
                        nav.navigate(s.route)
                    }
            ) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(top = 0.dp).size(110.dp)
                        .background(Color.White.copy(alpha = 0.10f), CircleShape)
                )
                Row(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(58.dp).background(Color.White.copy(alpha = 0.22f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Text(s.emoji, fontSize = 28.sp) }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.height(3.dp))
                        Text(s.sub, color = Color.White.copy(alpha = 0.92f), fontSize = 12.sp)
                    }
                    Box(
                        Modifier.background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) { Text("ببین", color = Brand.violetDeep, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                }
            }
        }
        DotsRow(promos.size, pager.currentPage)
    }
}

/** اسلایدر افقی پیشنهاد محصولات (از اطلاعات صفحه اصلی سایت) */
@Composable
fun SuggestSlider(nav: NavController, title: String = "پیشنهاد برای شما") {
    var list by remember { mutableStateOf<List<Product>>(emptyList()) }
    LaunchedEffect(Unit) {
        val h = withContext(Dispatchers.IO) { runCatching { Api.home() }.getOrNull() }
        if (h != null) list = (h.popular + h.sale + h.newest).distinctBy { it.id }.take(12)
    }
    if (list.isNotEmpty()) {
        Column(Modifier.fillMaxWidth()) {
            SectionHeader(title)
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(list) { p -> ProductCard(Modifier.width(160.dp), p, nav) }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

/** هدر گرادیانی صفحه‌های داخلی با دکمه بازگشت */
@Composable
fun GradientTopBar(title: String, nav: NavController) {
    Row(
        Modifier.fillMaxWidth()
            .background(Brand.headerGradient, RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp))
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { nav.popBackStack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت", tint = Color.White)
        }
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
    }
}

/** هدر گرادیانی صفحه‌های اصلی (سبد، علاقه‌مندی، حساب) */
@Composable
fun TabHeader(title: String, sub: String = "") {
    Column(
        Modifier.fillMaxWidth()
            .background(Brand.headerGradient, RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp))
            .padding(20.dp, 18.dp, 20.dp, 18.dp)
    ) {
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        if (sub.isNotBlank()) Text(sub, fontSize = 13.sp, color = Color.White.copy(alpha = 0.92f))
    }
}

/** حالت خالی (بدون fillMaxSize تا داخل اسکرول هم کار کند) */
@Composable
fun EmptyState(emoji: String, text: String) {
    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(96.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) { Text(emoji, fontSize = 44.sp) }
        Spacer(Modifier.height(14.dp))
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
    }
}

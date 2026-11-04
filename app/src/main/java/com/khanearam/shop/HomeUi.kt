package com.khanearam.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

// ---------- صفحه اصلی (تم صورتی و بنفش + اسلایدرها) ----------

private data class Slide(
    val image: String,
    val productId: Int,
    val title: String,
    val sub: String,
    val emoji: String,
    val brush: Brush,
    val route: String
)

@Composable
private fun SectionTitle(title: String, onAll: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(16.dp, 22.dp, 8.dp, 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(4.dp).height(20.dp).background(Brand.pink, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(8.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f))
        if (onAll != null) TextButton(onClick = onAll) { Text("مشاهده همه") }
    }
}

@Composable
private fun HeroSlider(h: Home, nav: NavController) {
    val slides = if (h.banners.isNotEmpty()) {
        h.banners.map { Slide(it.image, it.productId, "", "", "", Brand.promo1, "") }
    } else {
        listOf(
            Slide("", 0, "تخفیف‌های ویژه", "محصولات تخفیف‌دار را از دست ندهید", "🎀", Brand.promo2, "list/0"),
            Slide("", 0, "جدیدترین محصولات", "تازه‌های فروشگاه خانه آرام", "✨", Brand.promo1, "list/0"),
            Slide("", 0, "گردونه شانس", "هر روز یک شانس برای برنده شدن", "🎡", Brand.promo3, "wheel")
        )
    }
    val pager = rememberPagerState(pageCount = { slides.size })
    LaunchedEffect(slides.size) {
        if (slides.size > 1) {
            while (true) {
                delay(4500)
                if (!pager.isScrollInProgress) {
                    pager.animateScrollToPage((pager.currentPage + 1) % slides.size)
                }
            }
        }
    }
    Column(Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pager,
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) { i ->
            val s = slides[i]
            Box(
                Modifier.fillMaxWidth().height(172.dp).clip(RoundedCornerShape(22.dp)).clickable {
                    if (s.productId > 0) {
                        nav.navigate("product/${s.productId}")
                    } else if (s.route.isNotBlank()) {
                        Store.query = ""
                        nav.navigate(s.route)
                    }
                }
            ) {
                if (s.image.isNotBlank()) {
                    AsyncImage(
                        model = s.image,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(s.brush))
                    Box(
                        Modifier.align(Alignment.CenterStart).padding(start = 20.dp).size(88.dp)
                            .background(Color.White.copy(alpha = 0.22f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Text(s.emoji, fontSize = 42.sp) }
                    Column(Modifier.align(Alignment.CenterEnd).padding(end = 22.dp)) {
                        Text(s.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(s.sub, color = Color.White.copy(alpha = 0.92f), fontSize = 13.sp)
                    }
                }
            }
        }
        if (slides.size > 1) {
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                slides.indices.forEach { i ->
                    val on = pager.currentPage == i
                    Box(
                        Modifier.padding(horizontal = 3.dp).height(7.dp).width(if (on) 20.dp else 7.dp)
                            .background(if (on) Brand.pink else Brand.lavender, RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActions(nav: NavController) {
    val items = listOf(
        Triple("🎡", "گردونه شانس", "wheel"),
        Triple("👛", "کیف پول", "wallet"),
        Triple("📖", "مقالات", "articles"),
        Triple("✅", "اصالت کالا", "verify")
    )
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        items.forEach { (emoji, label, route) ->
            Column(
                Modifier.width(76.dp).clip(RoundedCornerShape(16.dp)).clickable { nav.navigate(route) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier.size(58.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) { Text(emoji, fontSize = 26.sp) }
                Spacer(Modifier.height(6.dp))
                Text(label, fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun CategoryCircles(h: Home, nav: NavController) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(h.categories) { c ->
            Column(
                Modifier.width(72.dp).clickable {
                    Store.query = ""
                    nav.navigate("list/${c.id}")
                },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier.size(66.dp).clip(CircleShape).background(Brand.headerGradient),
                    contentAlignment = Alignment.Center
                ) {
                    if (c.image.isNotBlank()) {
                        AsyncImage(
                            model = c.image,
                            contentDescription = c.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(c.name.take(1), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(c.name, fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ProductSlider(list: List<Product>, nav: NavController) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(list) { p -> ProductCard(Modifier.width(160.dp), p, nav) }
    }
}

@Composable
private fun PromoStrip(nav: NavController) {
    Box(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp).height(86.dp)
            .clip(RoundedCornerShape(20.dp)).background(Brand.promo3).clickable { nav.navigate("wheel") }
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🎁", fontSize = 34.sp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("باشگاه مشتریان خانه آرام", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("با هر خرید امتیاز بگیرید و جایزه ببرید", color = Color.White.copy(alpha = 0.92f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun HomeScreen(nav: NavController) {
    var q by remember { mutableStateOf("") }
    Loader(Unit, { Api.home() }) { h ->
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Column(
                    Modifier.fillMaxWidth()
                        .background(Brand.headerGradient, RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                        .padding(16.dp, 18.dp, 16.dp, 22.dp)
                ) {
                    Text("خانه آرام", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("فروشگاه محصولات زناشویی", fontSize = 13.sp, color = Color.White.copy(alpha = 0.9f))
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = q,
                        onValueChange = { q = it },
                        singleLine = true,
                        shape = RoundedCornerShape(28.dp),
                        placeholder = { Text("جستجوی محصول...") },
                        leadingIcon = { Icon(Icons.Filled.Search, null) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        trailingIcon = { SearchActions(nav) { t -> q = t; Store.query = t; nav.navigate("list/0") } },
                        keyboardActions = KeyboardActions(onSearch = {
                            Store.query = q
                            nav.navigate("list/0")
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color(0xFF2A1F33),
                            unfocusedTextColor = Color(0xFF2A1F33),
                            focusedPlaceholderColor = Color(0xFF7C6A8C),
                            unfocusedPlaceholderColor = Color(0xFF7C6A8C),
                            focusedLeadingIconColor = Brand.violetDeep,
                            unfocusedLeadingIconColor = Brand.violetDeep,
                            focusedTrailingIconColor = Brand.violetDeep,
                            unfocusedTrailingIconColor = Brand.violetDeep
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            if (h.notice.isNotBlank()) {
                item {
                    Card(
                        Modifier.fillMaxWidth().padding(16.dp, 14.dp, 16.dp, 0.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Text(
                            h.notice,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(12.dp),
                            fontSize = 14.sp
                        )
                    }
                }
            }
            item {
                Spacer(Modifier.height(16.dp))
                HeroSlider(h, nav)
            }
            item { QuickActions(nav) }
            if (h.categories.isNotEmpty()) {
                item {
                    SectionTitle("دسته‌بندی‌ها")
                    CategoryCircles(h, nav)
                }
            }
            if (h.sale.isNotEmpty()) {
                item {
                    SectionTitle("تخفیف‌دارها") {
                        Store.query = ""
                        nav.navigate("list/0")
                    }
                    ProductSlider(h.sale, nav)
                }
            }
            item { PromoStrip(nav) }
            if (h.popular.isNotEmpty()) {
                item {
                    SectionTitle("پرفروش‌ترین‌ها") {
                        Store.query = ""
                        nav.navigate("list/0")
                    }
                    ProductSlider(h.popular, nav)
                }
            }
            if (Store.recent.isNotEmpty()) {
                item {
                    SectionTitle("اخیراً دیده‌شده‌ها")
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(Store.recent.toList()) { f ->
                            Column(Modifier.width(100.dp).clickable { nav.navigate("product/${f.id}") }) {
                                AsyncImage(
                                    model = f.image,
                                    contentDescription = f.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(100.dp).clip(RoundedCornerShape(16.dp)).background(Color.White)
                                )
                                Text(f.name, maxLines = 1, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            if (h.newest.isNotEmpty()) {
                item { SectionTitle("جدیدترین‌ها") }
                items(h.newest.chunked(2)) { row -> ProductRow(row, nav) }
            }
            item {
                TextButton(onClick = {
                    Store.query = ""
                    nav.navigate("list/0")
                }, modifier = Modifier.fillMaxWidth().padding(8.dp)) { Text("مشاهده همه محصولات") }
            }
        }
    }
}

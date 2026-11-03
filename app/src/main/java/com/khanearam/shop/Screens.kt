@file:OptIn(ExperimentalMaterial3Api::class)

package com.khanearam.shop

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Switch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ---------- اجزای مشترک ----------

@Composable
fun <T> Loader(key: Any?, load: suspend () -> T, content: @Composable (T) -> Unit) {
    var state by remember(key) { mutableStateOf<Result<T>?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    LaunchedEffect(key, retry) {
        state = null
        state = withContext(Dispatchers.IO) { runCatching { load() } }
    }
    val s = state
    if (s == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else if (s.isFailure) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(s.exceptionOrNull()?.message ?: "خطا در دریافت اطلاعات", textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Button(onClick = { retry++ }) { Text("تلاش دوباره") }
        }
    } else {
        content(s.getOrThrow())
    }
}

@Composable
fun TopBack(title: String, nav: NavController) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { nav.popBackStack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
        }
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@Composable
fun Empty(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(text, color = Color.Gray) }
}

@Composable
fun PriceText(p: Product) {
    Column {
        if (p.onSale) {
            Text(money(p.price), fontSize = 12.sp, color = Color.Gray, textDecoration = TextDecoration.LineThrough)
        }
        Text(money(p.finalPrice), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun ProductCard(modifier: Modifier, p: Product, nav: NavController) {
    Card(
        modifier.clickable { nav.navigate("product/${p.id}") },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            Box {
                AsyncImage(
                    model = p.image,
                    contentDescription = p.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(140.dp)
                )
                Column(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (p.onSale) Badge2("تخفیف", Color(0xFFD32F2F))
                    if (p.isNew) Badge2("جدید", Color(0xFF2E7D32))
                    if (p.isPopular) Badge2("پرفروش", Color(0xFFEF6C00))
                }
            }
            Column(Modifier.padding(10.dp)) {
                Text(p.name, maxLines = 2, minLines = 2, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                PriceText(p)
                if (p.stock <= 0) Text(if (p.preorder) "پیش‌سفارش" else "ناموجود", color = if (p.preorder) Color(0xFFEF6C00) else Color.Red, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun ProductRow(row: List<Product>, nav: NavController) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        row.forEach { ProductCard(Modifier.weight(1f), it, nav) }
        if (row.size == 1) Spacer(Modifier.weight(1f))
    }
}

// ---------- صفحه اصلی ----------

@Composable
fun HomeScreen(nav: NavController) {
    var q by remember { mutableStateOf("") }
    Loader(Unit, { Api.home() }) { h ->
        LazyColumn(Modifier.fillMaxSize()) {
            if (h.notice.isNotBlank()) {
                item {
                    Card(Modifier.fillMaxWidth().padding(16.dp, 12.dp, 16.dp, 0.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
                        Text(h.notice, color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(12.dp), fontSize = 14.sp)
                    }
                }
            }
            item {
                Column(Modifier.padding(16.dp)) {
                    Text("خانه آرام", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("فروشگاه محصولات زناشویی", fontSize = 13.sp, color = Color.Gray)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = q,
                        onValueChange = { q = it },
                        singleLine = true,
                        placeholder = { Text("جستجوی محصول...") },
                        leadingIcon = { Icon(Icons.Filled.Search, null) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        trailingIcon = { SearchActions(nav) { t -> q = t; Store.query = t; nav.navigate("list/0") } },
                        keyboardActions = KeyboardActions(onSearch = {
                            Store.query = q
                            nav.navigate("list/0")
                        }),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            if (h.banners.isNotEmpty()) {
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(h.banners) { b ->
                            AsyncImage(
                                model = b.image,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.width(300.dp).height(150.dp).clip(RoundedCornerShape(16.dp)).clickable {
                                    if (b.productId > 0) nav.navigate("product/${b.productId}")
                                }
                            )
                        }
                    }
                }
            }
            if (h.categories.isNotEmpty()) {
                item {
                    Text("دسته‌بندی‌ها", fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 4.dp))
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(h.categories) { c ->
                            AssistChip(onClick = {
                                Store.query = ""
                                nav.navigate("list/${c.id}")
                            }, label = { Text(c.name) })
                        }
                    }
                }
            }
            if (Store.recent.isNotEmpty()) {
                item {
                    Text("اخیراً دیده‌شده‌ها", fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 4.dp))
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(Store.recent.toList()) { f ->
                            Column(Modifier.width(100.dp).clickable { nav.navigate("product/${f.id}") }) {
                                AsyncImage(
                                    model = f.image,
                                    contentDescription = f.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(100.dp).clip(RoundedCornerShape(10.dp))
                                )
                                Text(f.name, maxLines = 1, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            if (h.sale.isNotEmpty()) {
                item { Text("تخفیف‌دارها", fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 4.dp)) }
                items(h.sale.chunked(2)) { row -> ProductRow(row, nav) }
            }
            if (h.newest.isNotEmpty()) {
                item { Text("جدیدترین‌ها", fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 4.dp)) }
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

// ---------- لیست محصولات ----------

@Composable
fun ListScreen(nav: NavController, cat: Int) {
    var q by remember { mutableStateOf(Store.query) }
    var applied by remember { mutableStateOf(Store.query) }
    var sort by remember { mutableStateOf("newest") }
    var brand by remember { mutableIntStateOf(0) }
    var minP by remember { mutableStateOf("") }
    var maxP by remember { mutableStateOf("") }
    var showFilter by remember { mutableStateOf(false) }
    var brands by remember { mutableStateOf<List<Pair<Int, String>>>(emptyList()) }
    LaunchedEffect(Unit) {
        brands = withContext(Dispatchers.IO) { runCatching { Api.config().brands }.getOrDefault(emptyList()) }
    }
    var list by remember { mutableStateOf<List<Product>>(emptyList()) }
    var page by remember { mutableIntStateOf(1) }
    var pages by remember { mutableIntStateOf(1) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var more by remember { mutableIntStateOf(0) }
    val sorts = listOf("newest" to "جدیدترین", "cheap" to "ارزان‌ترین", "expensive" to "گران‌ترین", "popular" to "پرفروش‌ترین")

    LaunchedEffect(cat, applied, sort, brand, minP, maxP) {
        loading = true
        error = ""
        list = emptyList()
        page = 1
        val r = withContext(Dispatchers.IO) { runCatching { Api.products(cat, applied, sort, 1, brand, minP, maxP) } }
        r.onSuccess { list = it.list; pages = it.pages }.onFailure { error = it.message ?: "خطا" }
        loading = false
    }
    LaunchedEffect(more) {
        if (more > 0) {
            loading = true
            val next = page + 1
            val r = withContext(Dispatchers.IO) { runCatching { Api.products(cat, applied, sort, next, brand, minP, maxP) } }
            r.onSuccess { list = list + it.list; page = next; pages = it.pages }.onFailure { error = it.message ?: "خطا" }
            loading = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopBack("محصولات", nav)
        OutlinedTextField(
            value = q,
            onValueChange = { q = it },
            singleLine = true,
            placeholder = { Text("جستجو...") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            trailingIcon = { SearchActions(nav) { t -> q = t; applied = t } },
            keyboardActions = KeyboardActions(onSearch = { applied = q }),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )
        if (showFilter) {
            FilterDialog(brands, brand, minP, maxP, onApply = { b, mn, mx -> brand = b; minP = mn; maxP = mx; showFilter = false }, onDismiss = { showFilter = false })
        }
        OutlinedButton(onClick = { showFilter = true }, modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(if (brand != 0 || minP.isNotBlank() || maxP.isNotBlank()) "فیلتر برند و قیمت (فعال)" else "فیلتر برند و قیمت")
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(sorts) { (key, label) ->
                FilterChip(selected = sort == key, onClick = { sort = key }, label = { Text(label) })
            }
        }
        if (loading && list.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (error.isNotEmpty() && list.isEmpty()) {
            Empty(error)
        } else if (list.isEmpty()) {
            Empty("محصولی پیدا نشد")
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(list.chunked(2)) { row -> ProductRow(row, nav) }
                item {
                    if (page < pages) {
                        Button(onClick = { more++ }, enabled = !loading, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(if (loading) "در حال دریافت..." else "نمایش بیشتر")
                        }
                    }
                }
            }
        }
    }
}

// ---------- صفحه محصول ----------

@Composable
fun ProductScreen(nav: NavController, id: Int) {
    Column(Modifier.fillMaxSize()) {
        TopBack("جزئیات محصول", nav)
        Loader(id, { Api.product(id) }) { p ->
            var added by remember { mutableStateOf(false) }
            var quick by remember { mutableStateOf(false) }
            var notified by remember { mutableStateOf(false) }
            var compareMsg by remember { mutableStateOf(false) }
            LaunchedEffect(p.id) { Store.addRecent(p) }
            val pscope = rememberCoroutineScope()
            Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
                Gallery(if (p.images.isNotEmpty()) p.images else listOf(p.image))
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(p.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    if (p.categoryName.isNotEmpty()) Text(p.categoryName, color = Color.Gray)
                    PriceText(p)
                    if (p.saleLeft > 0) Countdown(p.saleLeft)
                    Text(
                        if (p.stock > 0) "موجود در انبار" else if (p.preorder) "ناموجود — قابل پیش‌سفارش" else "ناموجود",
                        color = if (p.stock > 0) Color(0xFF2E7D32) else if (p.preorder) Color(0xFFEF6C00) else Color.Red
                    )
                    if (p.stock <= 0 && p.preorder && p.preorderNote.isNotBlank()) Text(p.preorderNote, fontSize = 13.sp)
                    if (p.description.isNotBlank()) Text(p.description)
                    if (p.brand.isNotBlank()) Text("برند: ${p.brand}", fontSize = 13.sp)
                    if (p.expiry.isNotBlank()) Text("تاریخ انقضا: ${p.expiry}", fontSize = 13.sp)
                    if (p.ingredients.isNotBlank()) {
                        Text("ترکیبات", fontWeight = FontWeight.Bold)
                        Text(p.ingredients, fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = {
                                Store.addToCart(p)
                                added = true
                            },
                            enabled = p.stock > 0 || p.preorder,
                            modifier = Modifier.weight(1f)
                        ) { Text(if (p.stock > 0) "افزودن به سبد خرید" else "پیش‌سفارش") }
                        IconButton(onClick = { Store.toggleFav(p) }) {
                            Icon(
                                if (Store.isFav(p.id)) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = "علاقه‌مندی",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (p.stock > 0 && Store.loggedIn) {
                        OutlinedButton(onClick = { quick = true }, modifier = Modifier.fillMaxWidth()) { Text("خرید با یک کلیک") }
                    }
                    if (p.stock <= 0 && !p.preorder && Store.loggedIn) {
                        OutlinedButton(
                            enabled = !notified,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                pscope.launch {
                                    val r = withContext(Dispatchers.IO) { runCatching { Api.notifyStock(p.id) } }
                                    if (r.isSuccess) notified = true
                                }
                            }
                        ) { Text(if (notified) "وقتی موجود شد خبرتان می‌کنیم" else "موجود شد خبرم کن") }
                    }
                    OutlinedButton(
                        onClick = {
                            Store.addCompare(p)
                            if (Store.compare.size == 2) nav.navigate("compare") else compareMsg = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("مقایسه با محصول دیگر") }
                    if (compareMsg) Text("به مقایسه اضافه شد؛ محصول دوم را باز کنید و دوباره «مقایسه» را بزنید.", fontSize = 12.sp, color = Color.Gray)
                    if (quick) QuickBuyDialog(p, nav) { quick = false }
                    if (added) Text("به سبد خرید اضافه شد", color = Color(0xFF2E7D32))
                    if (p.related.isNotEmpty()) {
                        Text("محصولات مرتبط", fontWeight = FontWeight.Bold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(p.related) { rp -> ProductCard(Modifier.width(160.dp), rp, nav) }
                        }
                    }
                    ReviewsSection(p.id, nav)
                }
            }
        }
    }
}

// ---------- نظرات محصول ----------

@Composable
fun ReviewsSection(productId: Int, nav: NavController) {
    var refresh by remember { mutableIntStateOf(0) }
    var data by remember { mutableStateOf<ReviewData?>(null) }
    var rating by remember { mutableIntStateOf(5) }
    var text by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(productId, refresh) {
        data = withContext(Dispatchers.IO) { runCatching { Api.reviews(productId) }.getOrNull() }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider()
        Text("نظرات کاربران", fontWeight = FontWeight.Bold)
        val d = data
        if (d != null) {
            if (d.count > 0) {
                Text("امتیاز: ${d.avg} از ۵ (${d.count} نظر)")
            } else {
                Text("هنوز نظری ثبت نشده", color = Color.Gray)
            }
            d.reviews.forEach { rv ->
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(rv.name + "   " + "★".repeat(rv.rating.coerceIn(0, 5)), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        if (rv.text.isNotBlank()) Text(rv.text, fontSize = 14.sp)
                        Text(rv.date, color = Color.Gray, fontSize = 11.sp)
                    }
                }
            }
        }
        if (Store.loggedIn) {
            Text("امتیاز شما:")
            Row {
                (1..5).forEach { i ->
                    TextButton(onClick = { rating = i }) { Text(if (i <= rating) "★" else "☆", fontSize = 26.sp) }
                }
            }
            OutlinedTextField(text, { text = it }, label = { Text("نظر شما (اختیاری)") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            Button(
                enabled = !busy,
                onClick = {
                    busy = true
                    msg = ""
                    scope.launch {
                        val r = withContext(Dispatchers.IO) { runCatching { Api.addReview(productId, rating, text) } }
                        busy = false
                        r.onSuccess { msg = it; text = ""; refresh++ }
                            .onFailure { msg = it.message ?: "خطا" }
                    }
                }
            ) { Text("ثبت نظر") }
            if (msg.isNotEmpty()) Text(msg)
        } else {
            TextButton(onClick = { nav.navigate("auth") }) { Text("برای ثبت نظر وارد شوید") }
        }
    }
}

// ---------- سبد خرید ----------

@Composable
fun CartScreen(nav: NavController) {
    Column(Modifier.fillMaxSize()) {
        Text("سبد خرید", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
        if (Store.cart.isEmpty()) {
            Empty("سبد خرید شما خالی است")
        } else {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Store.cart.toList(), key = { it.productId }) { c ->
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = c.image,
                                contentDescription = c.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(70.dp).clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(c.name, maxLines = 2, fontSize = 14.sp)
                                Text(money(c.price), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(onClick = { Store.changeQty(c.productId, -1) }) { Text("−", fontSize = 20.sp) }
                                    Text("${c.qty}")
                                    IconButton(onClick = { Store.changeQty(c.productId, 1) }) { Icon(Icons.Filled.Add, "افزودن") }
                                }
                            }
                            IconButton(onClick = { Store.removeFromCart(c.productId) }) { Icon(Icons.Filled.Delete, "حذف") }
                        }
                    }
                }
            }
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("جمع کل", fontWeight = FontWeight.Bold)
                    Text(money(Store.cartTotal()), fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { nav.navigate(if (Store.loggedIn) "checkout" else "auth") },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("ادامه ثبت سفارش") }
            }
        }
    }
}

// ---------- ورود و ثبت‌نام ----------

@Composable
fun AuthScreen(nav: NavController) {
    var register by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var ref by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize()) {
        TopBack(if (register) "ثبت‌نام" else "ورود", nav)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (register) {
                OutlinedTextField(name, { name = it }, label = { Text("نام و نام خانوادگی") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            OutlinedTextField(
                phone, { phone = it },
                label = { Text("شماره موبایل (مثلاً 09123456789)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                pass, { pass = it },
                label = { Text("رمز عبور (حداقل ۶ حرف)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            if (register) {
                OutlinedTextField(ref, { ref = it }, label = { Text("کد دعوت (اختیاری)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            if (err.isNotEmpty()) Text(err, color = Color.Red)
            Button(
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    busy = true
                    err = ""
                    scope.launch {
                        val r = withContext(Dispatchers.IO) {
                            runCatching { if (register) Api.register(name, phone, pass, ref) else Api.login(phone, pass) }
                        }
                        busy = false
                        r.onSuccess { Store.login(it.first, it.second); nav.popBackStack() }
                            .onFailure { err = it.message ?: "خطا" }
                    }
                }
            ) { Text(if (register) "ثبت‌نام" else "ورود") }
            TextButton(onClick = { register = !register; err = "" }) {
                Text(if (register) "حساب دارید؟ ورود" else "حساب ندارید؟ ثبت‌نام")
            }
        }
    }
}

// ---------- علاقه‌مندی‌ها ----------

@Composable
fun FavsScreen(nav: NavController) {
    Column(Modifier.fillMaxSize()) {
        Text("علاقه‌مندی‌ها", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
        if (Store.favs.isEmpty()) {
            Empty("لیست علاقه‌مندی‌ها خالی است")
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Store.favs.toList(), key = { it.id }) { f ->
                    Card(
                        Modifier.fillMaxWidth().clickable { nav.navigate("product/${f.id}") },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = f.image,
                                contentDescription = f.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(70.dp).clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(f.name, maxLines = 2, fontSize = 14.sp)
                                Text(money(f.price), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = { Store.removeFav(f.id) }) { Icon(Icons.Filled.Delete, "حذف") }
                        }
                    }
                }
            }
        }
    }
}

// ---------- حساب من ----------

@Composable
fun AccountScreen(nav: NavController) {
    val ctx = LocalContext.current
    var cfg by remember { mutableStateOf<Config?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var me by remember { mutableStateOf<Me?>(null) }
    var accMsg by remember { mutableStateOf("") }
    var showBirthday by remember { mutableStateOf(false) }
    fun reload() {
        scope.launch { me = withContext(Dispatchers.IO) { runCatching { Api.me() }.getOrNull() } }
    }
    LaunchedEffect(Unit) {
        cfg = withContext(Dispatchers.IO) { runCatching { Api.config() }.getOrNull() }
    }
    LaunchedEffect(Store.loggedIn) {
        me = if (Store.loggedIn) withContext(Dispatchers.IO) { runCatching { Api.me() }.getOrNull() } else null
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("حساب من", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        if (Store.loggedIn) {
            Text("سلام ${Store.userName}")
            val m = me
            if (m != null) {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("امتیاز باشگاه مشتریان: ${m.points}", fontWeight = FontWeight.Bold)
                        Text("اعتبار کیف پول: ${money(m.wallet.toLong())}", fontSize = 13.sp)
                        Text("کد دعوت شما: ${m.refCode}", fontSize = 13.sp)
                        if (m.levelName.isNotBlank()) {
                            Text("سطح شما: ${m.levelName}" + (if (m.levelPct > 0) " (${m.levelPct}٪ تخفیف دائمی)" else ""), fontSize = 13.sp)
                        }
                    }
                }
                if (!m.dailyClaimed) {
                    Button(
                        onClick = {
                            scope.launch {
                                val r = withContext(Dispatchers.IO) { runCatching { Api.daily() } }
                                accMsg = r.fold({ "$it امتیاز جایزه امروز را گرفتید" }, { it.message ?: "خطا" })
                                reload()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("دریافت جایزه امروز") }
                }
                if (m.birthday.isBlank()) {
                    OutlinedButton(onClick = { showBirthday = true }, modifier = Modifier.fillMaxWidth()) { Text("ثبت تاریخ تولد (هدیه تولد)") }
                }
                if (accMsg.isNotEmpty()) Text(accMsg, color = Color(0xFF2E7D32))
                OutlinedButton(onClick = { nav.navigate("wheel") }, modifier = Modifier.fillMaxWidth()) { Text("گردونه شانس") }
                OutlinedButton(
                    onClick = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "با کد دعوت ${m.refCode} در فروشگاه خانه آرام ثبت‌نام کن و امتیاز هدیه بگیر")
                        }
                        ctx.startActivity(Intent.createChooser(send, null))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("دعوت دوستان") }
            }
            OutlinedButton(onClick = { nav.navigate("orders") }, modifier = Modifier.fillMaxWidth()) { Text("سفارش‌های من") }
            OutlinedButton(onClick = { nav.navigate("wallet") }, modifier = Modifier.fillMaxWidth()) { Text("کیف پول و کارت هدیه") }
            OutlinedButton(onClick = { nav.navigate("addresses") }, modifier = Modifier.fillMaxWidth()) { Text("آدرس‌های من") }
            OutlinedButton(onClick = { nav.navigate("family") }, modifier = Modifier.fillMaxWidth()) { Text("اعضای خانواده") }
            OutlinedButton(onClick = { nav.navigate("support") }, modifier = Modifier.fillMaxWidth()) { Text("پشتیبانی و مشاوره") }
            OutlinedButton(onClick = { Store.logout() }, modifier = Modifier.fillMaxWidth()) { Text("خروج از حساب") }
            TextButton(onClick = { confirmDelete = true }) { Text("حذف حساب کاربری", color = Color.Red) }
        } else {
            Button(onClick = { nav.navigate("auth") }, modifier = Modifier.fillMaxWidth()) { Text("ورود / ثبت‌نام") }
        }
        if (msg.isNotEmpty()) Text(msg, color = Color.Red)
        OutlinedButton(onClick = { nav.navigate("verify") }, modifier = Modifier.fillMaxWidth()) { Text("تأیید اصالت کالا با کد") }
        OutlinedButton(onClick = { nav.navigate("lock") }, modifier = Modifier.fillMaxWidth()) { Text("قفل اپ و اثر انگشت") }
        OutlinedButton(onClick = { nav.navigate("articles") }, modifier = Modifier.fillMaxWidth()) { Text("مقالات آموزشی") }
        OutlinedButton(onClick = { nav.navigate("about") }, modifier = Modifier.fillMaxWidth()) { Text("درباره ما، قوانین و حریم خصوصی") }
        OutlinedButton(onClick = { nav.navigate("faq") }, modifier = Modifier.fillMaxWidth()) { Text("سؤال‌های متداول") }
        OutlinedButton(onClick = { nav.navigate("notif") }, modifier = Modifier.fillMaxWidth()) { Text("تنظیمات اعلان‌ها") }
        HorizontalDivider()
        val c = cfg
        if (c != null) {
            if (c.phone.isNotBlank()) {
                OutlinedButton(
                    onClick = { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + c.phone))) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Call, null)
                    Spacer(Modifier.width(8.dp))
                    Text("تماس با پشتیبانی")
                }
            }
            if (c.whatsapp.isNotBlank()) {
                OutlinedButton(
                    onClick = {
                        val num = c.whatsapp.filter { it.isDigit() }
                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$num")))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("پیام در واتساپ") }
            }
            if (c.about.isNotBlank()) Text(c.about)
        }
    }
    if (showBirthday) BirthdayDialog(onDone = { showBirthday = false; reload() }, onDismiss = { showBirthday = false })
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("حذف حساب") },
            text = { Text("با حذف حساب، اطلاعات ورود شما پاک می‌شود. مطمئن هستید؟") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch {
                        val r = withContext(Dispatchers.IO) { runCatching { Api.deleteAccount() } }
                        if (r.isSuccess) Store.logout() else msg = r.exceptionOrNull()?.message ?: "خطا"
                    }
                }) { Text("حذف") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("انصراف") } }
        )
    }
}

// ---------- گردونه شانس ----------

@Composable
fun WheelScreen(nav: NavController) {
    Column(Modifier.fillMaxSize()) {
        TopBack("گردونه شانس", nav)
        Loader(Unit, { Api.wheel() }) { w ->
            var shown by remember { mutableStateOf("؟") }
            var spun by remember { mutableStateOf(w.spunToday) }
            var points by remember { mutableIntStateOf(w.points) }
            var spinning by remember { mutableStateOf(false) }
            var msg by remember { mutableStateOf("") }
            val scope = rememberCoroutineScope()
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("امتیاز شما: $points", fontSize = 18.sp)
                Card(Modifier.fillMaxWidth().height(140.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(shown, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                    }
                }
                Button(
                    enabled = !spun && !spinning && w.labels.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        spinning = true
                        msg = ""
                        scope.launch {
                            val r = withContext(Dispatchers.IO) { runCatching { Api.spin() } }
                            r.onSuccess { res ->
                                repeat(18) {
                                    shown = w.labels.random()
                                    delay(90)
                                }
                                shown = res.label
                                points = res.points
                                spun = true
                                msg = if (res.won > 0) "تبریک! ${res.won} امتیاز گرفتید" else "این بار شانس با شما نبود؛ فردا دوباره امتحان کنید"
                            }.onFailure { msg = it.message ?: "خطا" }
                            spinning = false
                        }
                    }
                ) { Text(if (spun) "امروز چرخانده‌اید" else "بچرخان") }
                if (msg.isNotEmpty()) Text(msg, textAlign = TextAlign.Center)
                Text(
                    "هر روز یک بار می‌توانید بچرخانید. امتیازها در ثبت سفارش، به‌جای پول استفاده می‌شوند.",
                    color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center
                )
            }
        }
    }
}

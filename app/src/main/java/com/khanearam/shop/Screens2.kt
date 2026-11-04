@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.khanearam.shop

import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ---------- ابزارهای مشترک ----------

@Composable
fun Picker(label: String, current: String, options: List<Pair<String, String>>, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }) {
            Text(if (current.isBlank()) label else (options.firstOrNull { it.first == current }?.second ?: current))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (v, l) ->
                DropdownMenuItem(text = { Text(l) }, onClick = { onPick(v); open = false })
            }
        }
    }
}

@Composable
fun cardColors() = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)

// ---------- گالری و زوم ----------

@Composable
fun Gallery(images: List<String>) {
    var zoom by remember { mutableStateOf<String?>(null) }
    val pager = rememberPagerState(pageCount = { images.size })
    Box {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxWidth().height(300.dp)) { i ->
            AsyncImage(
                model = images[i],
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clickable { zoom = images[i] }
            )
        }
        if (images.size > 1) {
            Text(
                "${pager.currentPage + 1} / ${images.size}",
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp)
                    .background(Color(0x99000000), RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 2.dp)
            )
        }
    }
    val z = zoom
    if (z != null) {
        Dialog(onDismissRequest = { zoom = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            ZoomImage(z) { zoom = null }
        }
    }
}

@Composable
fun ZoomImage(url: String, onClose: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoomChange, _ ->
                        scale = (scale * zoomChange).coerceIn(1f, 5f)
                        offset = if (scale > 1f) offset + pan else Offset.Zero
                    }
                }
                .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y)
        )
        IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
            Icon(Icons.Filled.Close, contentDescription = "بستن", tint = Color.White)
        }
    }
}

// ---------- خرید با یک کلیک ----------

data class QuickInfo(val addr: Address, val method: ShipMethod, val onlinePay: Boolean, val slotDate: String, val slot: String)

@Composable
fun QuickBuyDialog(p: Product, nav: NavController, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var info by remember { mutableStateOf<Result<QuickInfo>?>(null) }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf("") }
    val item = CartItem(p.id, p.name, p.finalPrice, p.image, 1)

    LaunchedEffect(Unit) {
        info = withContext(Dispatchers.IO) {
            runCatching {
                val addrs = Api.addresses()
                val a = addrs.firstOrNull { it.isDefault } ?: addrs.firstOrNull()
                    ?: throw ApiError("ابتدا از بخش «آدرس‌های من» یک آدرس ذخیره کنید")
                val cfg = Api.config()
                val ms = Api.quote(listOf(item), a.city)
                val m = ms.filter { it.type == "ship" }.minByOrNull { it.cost } ?: ms.first()
                QuickInfo(a, m, cfg.onlinePay, cfg.slotDates.firstOrNull()?.value ?: "", cfg.slots.firstOrNull() ?: "")
            }
        }
    }

    fun order(online: Boolean, i: QuickInfo) {
        busy = true
        err = ""
        scope.launch {
            val r = withContext(Dispatchers.IO) {
                runCatching {
                    Api.createOrder(
                        OrderForm(listOf(item), i.addr.name, i.addr.phone, i.addr.address, i.addr.city, "", "", online, 0, i.method.id, i.slotDate, i.slot, "")
                    )
                }
            }
            busy = false
            r.onSuccess { res ->
                onDismiss()
                nav.navigate("orders")
                if (res.payUrl.isNotBlank()) ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(res.payUrl)))
            }.onFailure { err = it.message ?: "خطا" }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("خرید با یک کلیک") },
        text = {
            val r = info
            if (r == null) {
                Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (r.isFailure) {
                Text(r.exceptionOrNull()?.message ?: "خطا")
            } else {
                val i = r.getOrThrow()
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(p.name, fontWeight = FontWeight.Bold)
                    Text("ارسال به: ${i.addr.city}، ${i.addr.address}", fontSize = 13.sp)
                    Text("روش ارسال: ${i.method.title} (${if (i.method.cost == 0L) "رایگان" else money(i.method.cost)})", fontSize = 13.sp)
                    Text("مبلغ کل: ${money(p.finalPrice + i.method.cost)}", fontWeight = FontWeight.Bold)
                    if (err.isNotEmpty()) Text(err, color = Color.Red)
                    Button(enabled = !busy, onClick = { order(false, i) }, modifier = Modifier.fillMaxWidth()) { Text("خرید و پرداخت در محل") }
                    if (i.onlinePay) {
                        OutlinedButton(enabled = !busy, onClick = { order(true, i) }, modifier = Modifier.fillMaxWidth()) { Text("خرید و پرداخت آنلاین") }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

// ---------- ثبت سفارش ----------

data class CheckoutData(val cfg: Config, val me: Me, val addrs: List<Address>, val members: List<Member>)

@Composable
fun CheckoutScreen(nav: NavController) {
    Column(Modifier.fillMaxSize()) {
        TopBack("ثبت سفارش", nav)
        Loader(Unit, { CheckoutData(Api.config(), Api.me(), Api.addresses(), Api.family()) }) { d -> CheckoutForm(nav, d) }
    }
}

@Composable
fun CheckoutForm(nav: NavController, d: CheckoutData) {
    val cfg = d.cfg
    val me = d.me
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(Store.userName) }
    var phone by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var quoteCity by remember { mutableStateOf("") }
    var otherMode by remember { mutableStateOf(false) }
    var address by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var member by remember { mutableStateOf("") }
    var methods by remember { mutableStateOf<List<ShipMethod>>(emptyList()) }
    var quoteErr by remember { mutableStateOf("") }
    var methodId by remember { mutableIntStateOf(0) }
    var slotDate by remember { mutableStateOf(cfg.slotDates.firstOrNull()?.value ?: "") }
    var slot by remember { mutableStateOf(cfg.slots.firstOrNull() ?: "") }
    var code by remember { mutableStateOf("") }
    var applied by remember { mutableStateOf("") }
    var percent by remember { mutableIntStateOf(0) }
    var usePts by remember { mutableStateOf(false) }
    var useWallet by remember { mutableStateOf(false) }
    var gift by remember { mutableStateOf(false) }
    var giftMsg by remember { mutableStateOf("") }
    var online by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf("") }

    fun applyAddr(a: Address) {
        name = a.name
        phone = a.phone
        city = a.city
        address = a.address
        quoteCity = a.city
        otherMode = a.city !in cfg.cities
    }

    LaunchedEffect(Unit) {
        d.addrs.firstOrNull { it.isDefault }?.let { applyAddr(it) }
    }
    LaunchedEffect(quoteCity) {
        quoteErr = ""
        val r = withContext(Dispatchers.IO) { runCatching { Api.quote(Store.cart.toList(), quoteCity) } }
        r.onSuccess { ms ->
            methods = ms
            if (ms.none { it.id == methodId }) methodId = ms.firstOrNull()?.id ?: 0
        }.onFailure {
            methods = emptyList()
            methodId = 0
            quoteErr = it.message ?: "خطا"
        }
    }

    val sel = methods.firstOrNull { it.id == methodId }
    val pickup = sel?.type == "pickup"
    val shipping = sel?.cost ?: 0L
    val subtotal = Store.cartTotal()
    val tierPct = cfg.tiers.filter { subtotal >= it.first }.maxOfOrNull { it.second } ?: 0
    val autoPct = maxOf(tierPct, me.levelPct)
    val autoDisc = subtotal * autoPct / 100
    val discount = (subtotal - autoDisc) * percent / 100
    val after = subtotal - autoDisc - discount
    val pv = if (cfg.pointValue > 0) cfg.pointValue else 1L
    val ptsUse = if (usePts) minOf(me.points.toLong(), (after + pv - 1) / pv) else 0L
    val ptsDisc = minOf(ptsUse * pv, after)
    val giftFee = if (gift) cfg.giftFee else 0L
    val total = after - ptsDisc + shipping + giftFee
    val walletUse = if (useWallet) minOf(me.wallet.toLong(), total) else 0L
    val payable = total - walletUse

    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (d.addrs.isNotEmpty()) {
            Text("انتخاب از آدرس‌های ذخیره‌شده", fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(d.addrs) { a -> FilterChip(selected = false, onClick = { applyAddr(a) }, label = { Text(a.title) }) }
            }
        }
        OutlinedTextField(name, { name = it }, label = { Text("نام گیرنده") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            phone, { phone = it },
            label = { Text("شماره موبایل گیرنده") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )

        Text("شهر", fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (cfg.cities.isNotEmpty()) {
                Picker("انتخاب شهر", if (otherMode) "" else city, cfg.cities.map { it to it }) {
                    city = it
                    quoteCity = it
                    otherMode = false
                }
            }
            if (cfg.otherCities) {
                OutlinedButton(onClick = { otherMode = true; city = ""; quoteCity = "" }) { Text("سایر شهرها") }
            }
        }
        if (otherMode) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(city, { city = it }, label = { Text("نام شهر") }, singleLine = true, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Button(onClick = { quoteCity = city.trim() }) { Text("محاسبه ارسال") }
            }
        }
        if (!pickup) {
            OutlinedTextField(address, { address = it }, label = { Text("آدرس کامل (منطقه، خیابان، پلاک)") }, minLines = 2, modifier = Modifier.fillMaxWidth())
        }

        Text("روش ارسال", fontWeight = FontWeight.Bold)
        if (methods.isEmpty()) {
            Text(if (quoteCity.isBlank()) "ابتدا شهر را انتخاب کنید" else quoteErr, color = Color.Gray)
        }
        methods.forEach { m ->
            Row(Modifier.fillMaxWidth().clickable { methodId = m.id }, verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = methodId == m.id, onClick = { methodId = m.id })
                Text("${m.title} — ${if (m.cost == 0L) "رایگان" else money(m.cost)}")
            }
        }

        if (cfg.slotDates.isNotEmpty() && cfg.slots.isNotEmpty()) {
            Text(if (pickup) "زمان مراجعه" else "زمان تحویل", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Picker("روز", slotDate, cfg.slotDates.map { it.value to it.label }) { slotDate = it }
                Picker("ساعت", slot, cfg.slots.map { it to it }) { slot = it }
            }
        }

        if (d.members.isNotEmpty()) {
            Text("سفارش برای (اختیاری)", fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(d.members) { m ->
                    FilterChip(selected = member == m.name, onClick = { member = if (member == m.name) "" else m.name }, label = { Text(m.name) })
                }
            }
        }
        OutlinedTextField(note, { note = it }, label = { Text("توضیحات (اختیاری)") }, modifier = Modifier.fillMaxWidth())

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(code, { code = it }, label = { Text("کد تخفیف") }, singleLine = true, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = {
                scope.launch {
                    val r = withContext(Dispatchers.IO) { runCatching { Api.checkCoupon(code.trim()) } }
                    r.onSuccess { percent = it; applied = code.trim(); err = "" }
                        .onFailure { percent = 0; applied = ""; err = it.message ?: "کد نامعتبر است" }
                }
            }) { Text("اعمال") }
        }
        if (percent > 0) Text("کد تخفیف اعمال شد: $percent درصد", color = Color(0xFF2E7D32))

        if (me.points > 0 && cfg.pointValue > 0) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = usePts, onCheckedChange = { usePts = it })
                Spacer(Modifier.width(8.dp))
                Text("استفاده از امتیاز باشگاه (${me.points} امتیاز، هر امتیاز ${money(cfg.pointValue)})", modifier = Modifier.weight(1f))
            }
        }

        if (cfg.giftFee > 0) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = gift, onCheckedChange = { gift = it })
                Spacer(Modifier.width(8.dp))
                Text("بسته‌بندی هدیه (${money(cfg.giftFee)})", modifier = Modifier.weight(1f))
            }
            if (gift) {
                OutlinedTextField(giftMsg, { giftMsg = it }, label = { Text("متن کارت هدیه (اختیاری)") }, modifier = Modifier.fillMaxWidth())
            }
        }
        if (me.wallet > 0) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = useWallet, onCheckedChange = { useWallet = it })
                Spacer(Modifier.width(8.dp))
                Text("استفاده از اعتبار کیف پول (${money(me.wallet.toLong())})", modifier = Modifier.weight(1f))
            }
        }
        HorizontalDivider()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("جمع کالاها"); Text(money(subtotal)) }
        if (autoDisc > 0) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("تخفیف ویژه ($autoPct٪)"); Text(money(autoDisc)) }
        if (discount > 0) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("تخفیف کد"); Text(money(discount)) }
        if (ptsDisc > 0) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("تخفیف امتیاز"); Text(money(ptsDisc)) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("هزینه ارسال"); Text(if (shipping == 0L) "رایگان" else money(shipping)) }
        if (giftFee > 0) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("بسته‌بندی هدیه"); Text(money(giftFee)) }
        if (walletUse > 0) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("جمع سفارش"); Text(money(total)) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("از کیف پول"); Text(money(walletUse)) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("مبلغ قابل پرداخت", fontWeight = FontWeight.Bold)
            Text(money(payable), fontWeight = FontWeight.Bold)
        }

        if (cfg.onlinePay && payable >= 1000) {
            Text("روش پرداخت", fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth().clickable { online = true }, verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = online, onClick = { online = true })
                Text("پرداخت آنلاین (درگاه بانکی)")
            }
            Row(Modifier.fillMaxWidth().clickable { online = false }, verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = !online, onClick = { online = false })
                Text("پرداخت در محل (هنگام تحویل)")
            }
        } else {
            Text(if (payable == 0L) "پرداخت: کامل از کیف پول" else "روش پرداخت: پرداخت در محل (هنگام تحویل)", color = Color.Gray)
        }
        if (err.isNotEmpty()) Text(err, color = Color.Red)
        if (saved.isNotEmpty()) Text(saved, color = Color(0xFF2E7D32))

        Button(
            enabled = !busy && Store.cart.isNotEmpty() && methodId != 0,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (name.isBlank() || phone.isBlank() || (!pickup && (city.isBlank() || address.isBlank()))) {
                    err = "نام، موبایل، شهر و آدرس را کامل وارد کنید"
                } else {
                    busy = true
                    err = ""
                    scope.launch {
                        val form = OrderForm(
                            Store.cart.toList(), name, phone, address, city, note, applied,
                            online && cfg.onlinePay && payable >= 1000, ptsUse.toInt(), methodId, slotDate, slot, member,
                            walletUse > 0, gift, giftMsg
                        )
                        val r = withContext(Dispatchers.IO) { runCatching { Api.createOrder(form) } }
                        busy = false
                        r.onSuccess { res ->
                            Store.clearCart()
                            nav.navigate("orders") { popUpTo("home") }
                            if (res.payUrl.isNotBlank()) ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(res.payUrl)))
                        }.onFailure { err = it.message ?: "ثبت سفارش انجام نشد" }
                    }
                }
            }
        ) { Text("ثبت نهایی سفارش") }

        if (!pickup && address.isNotBlank() && city.isNotBlank()) {
            TextButton(onClick = {
                scope.launch {
                    val r = withContext(Dispatchers.IO) {
                        runCatching { Api.saveAddress(Address("", city, name, phone, city, address, d.addrs.isEmpty())) }
                    }
                    saved = if (r.isSuccess) "آدرس ذخیره شد" else (r.exceptionOrNull()?.message ?: "خطا")
                }
            }) { Text("ذخیره این آدرس برای دفعات بعد") }
        }
    }
}

// ---------- سفارش‌ها ----------

@Composable
fun OrdersScreen(nav: NavController) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var tick by remember { mutableIntStateOf(0) }
    var msg by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var cfg by remember { mutableStateOf<Config?>(null) }
    var editing by remember { mutableStateOf<OrderRow?>(null) }
    var returning by remember { mutableStateOf<OrderRow?>(null) }
    var surveying by remember { mutableStateOf<OrderRow?>(null) }
    LaunchedEffect(Unit) {
        cfg = withContext(Dispatchers.IO) { runCatching { Api.config() }.getOrNull() }
    }
    Column(Modifier.fillMaxSize()) {
        TopBack("سفارش‌های من", nav)
        if (msg.isNotEmpty()) Text(msg, color = Color.Red, modifier = Modifier.padding(horizontal = 16.dp))
        Loader(tick, { Api.orders() }) { list ->
            if (list.isEmpty()) {
                Empty("هنوز سفارشی ثبت نکرده‌اید")
            } else {
                LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(list) { o ->
                        Card(Modifier.fillMaxWidth(), colors = cardColors()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("سفارش شماره ${o.id}", fontWeight = FontWeight.Bold)
                                Text(o.date, color = Color.Gray, fontSize = 12.sp)
                                Text("وضعیت: ${o.status}", color = MaterialTheme.colorScheme.primary)
                                if (o.returnStatus.isNotBlank()) Text("مرجوعی: ${o.returnStatus}", color = MaterialTheme.colorScheme.primary)
                                if (o.slotDate.isNotBlank()) Text("زمان تحویل: ${o.slotDate} — ${o.slot}", fontSize = 13.sp)
                                Text(o.items, maxLines = 3, fontSize = 13.sp)
                                Text("مبلغ کل: ${money(o.total)}", fontWeight = FontWeight.Bold)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (o.canPay) {
                                        Button(enabled = !busy, onClick = {
                                            busy = true
                                            msg = ""
                                            scope.launch {
                                                val r = withContext(Dispatchers.IO) { runCatching { Api.payOrder(o.id) } }
                                                busy = false
                                                r.onSuccess { url -> if (url.isNotBlank()) ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                                                    .onFailure { msg = it.message ?: "خطا" }
                                            }
                                        }) { Text("پرداخت") }
                                    }
                                    if (o.canEdit) OutlinedButton(onClick = { editing = o }) { Text("ویرایش") }
                                    if (o.canCancel) {
                                        OutlinedButton(enabled = !busy, onClick = {
                                            busy = true
                                            msg = ""
                                            scope.launch {
                                                val r = withContext(Dispatchers.IO) { runCatching { Api.cancelOrder(o.id) } }
                                                busy = false
                                                r.onSuccess { tick++ }.onFailure { msg = it.message ?: "خطا" }
                                            }
                                        }) { Text("لغو سفارش") }
                                    }
                                    if (o.canReturn) OutlinedButton(onClick = { returning = o }) { Text("مرجوعی") }
                                    if (o.canSurvey) OutlinedButton(onClick = { surveying = o }) { Text("نظرسنجی") }
                                    OutlinedButton(onClick = {
                                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(o.invoiceUrl)))
                                    }) { Text("فاکتور") }
                                    OutlinedButton(enabled = !busy && o.lines.isNotEmpty(), onClick = {
                                        busy = true
                                        msg = ""
                                        scope.launch {
                                            val r = withContext(Dispatchers.IO) {
                                                runCatching { o.lines.map { (id, qty) -> Pair(Api.product(id), qty) } }
                                            }
                                            busy = false
                                            r.onSuccess { ps ->
                                                ps.forEach { (p, qty) -> if (p.stock > 0) Store.addToCart(p, qty) }
                                                nav.navigate("cart")
                                            }.onFailure { msg = it.message ?: "یکی از محصولات دیگر موجود نیست" }
                                        }
                                    }) { Text("سفارش دوباره") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    val e = editing
    if (e != null) EditOrderDialog(e, cfg, onDone = { editing = null; tick++ }, onDismiss = { editing = null })
    val rt = returning
    if (rt != null) ReturnDialog(rt, onDone = { returning = null; tick++ }, onDismiss = { returning = null })
    val sv = surveying
    if (sv != null) SurveyDialog(sv, onDone = { surveying = null; tick++ }, onDismiss = { surveying = null })
}

@Composable
fun EditOrderDialog(o: OrderRow, cfg: Config?, onDone: () -> Unit, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(o.name) }
    var phone by remember { mutableStateOf(o.phone) }
    var address by remember { mutableStateOf(o.address) }
    var note by remember { mutableStateOf(o.note) }
    var slotDate by remember { mutableStateOf(o.slotDate.ifBlank { cfg?.slotDates?.firstOrNull()?.value ?: "" }) }
    var slot by remember { mutableStateOf(o.slot.ifBlank { cfg?.slots?.firstOrNull() ?: "" }) }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ویرایش سفارش ${o.id}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("اقلام سفارش قابل ویرایش نیست؛ برای تغییر اقلام، سفارش را لغو و دوباره ثبت کنید.", fontSize = 12.sp, color = Color.Gray)
                OutlinedTextField(name, { name = it }, label = { Text("نام گیرنده") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    phone, { phone = it }, label = { Text("موبایل") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(address, { address = it }, label = { Text("آدرس") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(note, { note = it }, label = { Text("توضیحات") }, modifier = Modifier.fillMaxWidth())
                if (cfg != null && cfg.slotDates.isNotEmpty() && cfg.slots.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Picker("روز", slotDate, cfg.slotDates.map { it.value to it.label }) { slotDate = it }
                        Picker("ساعت", slot, cfg.slots.map { it to it }) { slot = it }
                    }
                }
                if (err.isNotEmpty()) Text(err, color = Color.Red)
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = {
                busy = true
                err = ""
                scope.launch {
                    val r = withContext(Dispatchers.IO) { runCatching { Api.editOrder(o.id, name, phone, address, note, slotDate, slot) } }
                    busy = false
                    r.onSuccess { onDone() }.onFailure { err = it.message ?: "خطا" }
                }
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
fun ReturnDialog(o: OrderRow, onDone: () -> Unit, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var reason by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("درخواست مرجوعی سفارش ${o.id}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("دلیل مرجوعی را بنویسید. بعد از بررسی، نتیجه را در همین صفحه می‌بینید.", fontSize = 13.sp)
                OutlinedTextField(reason, { reason = it }, label = { Text("دلیل مرجوعی") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                if (err.isNotEmpty()) Text(err, color = Color.Red)
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = {
                busy = true
                err = ""
                scope.launch {
                    val r = withContext(Dispatchers.IO) { runCatching { Api.requestReturn(o.id, reason) } }
                    busy = false
                    r.onSuccess { onDone() }.onFailure { err = it.message ?: "خطا" }
                }
            }) { Text("ارسال درخواست") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

// ---------- آدرس‌ها ----------

@Composable
fun AddressesScreen(nav: NavController) {
    val scope = rememberCoroutineScope()
    var tick by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<Address?>(null) }
    Column(Modifier.fillMaxSize()) {
        TopBack("آدرس‌های من", nav)
        Button(
            onClick = { editing = Address("", "", "", "", "", "", false) },
            modifier = Modifier.fillMaxWidth().padding(16.dp, 4.dp)
        ) { Text("افزودن آدرس") }
        Loader(tick, { Api.addresses() }) { list ->
            if (list.isEmpty()) {
                Empty("آدرسی ذخیره نشده است")
            } else {
                LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(list, key = { it.id }) { a ->
                        Card(Modifier.fillMaxWidth(), colors = cardColors()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(a.title + if (a.isDefault) " (پیش‌فرض)" else "", fontWeight = FontWeight.Bold)
                                Text("${a.name} — ${a.phone}", fontSize = 13.sp)
                                Text("${a.city}، ${a.address}", fontSize = 13.sp)
                                Row {
                                    TextButton(onClick = { editing = a }) { Text("ویرایش") }
                                    TextButton(onClick = {
                                        scope.launch {
                                            withContext(Dispatchers.IO) { runCatching { Api.deleteAddress(a.id) } }
                                            tick++
                                        }
                                    }) { Text("حذف", color = Color.Red) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    val e = editing
    if (e != null) AddressDialog(e, onDone = { editing = null; tick++ }, onDismiss = { editing = null })
}

@Composable
fun AddressDialog(a: Address, onDone: () -> Unit, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf(a.title) }
    var name by remember { mutableStateOf(a.name) }
    var phone by remember { mutableStateOf(a.phone) }
    var city by remember { mutableStateOf(a.city) }
    var address by remember { mutableStateOf(a.address) }
    var def by remember { mutableStateOf(a.isDefault) }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (a.id.isEmpty()) "آدرس جدید" else "ویرایش آدرس") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("عنوان (مثلاً خانه)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(name, { name = it }, label = { Text("نام گیرنده") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    phone, { phone = it }, label = { Text("موبایل") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(city, { city = it }, label = { Text("شهر") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(address, { address = it }, label = { Text("آدرس کامل") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = def, onCheckedChange = { def = it })
                    Spacer(Modifier.width(8.dp))
                    Text("آدرس پیش‌فرض")
                }
                if (err.isNotEmpty()) Text(err, color = Color.Red)
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = {
                busy = true
                err = ""
                scope.launch {
                    val r = withContext(Dispatchers.IO) { runCatching { Api.saveAddress(Address(a.id, title, name, phone, city, address, def)) } }
                    busy = false
                    r.onSuccess { onDone() }.onFailure { err = it.message ?: "خطا" }
                }
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

// ---------- اعضای خانواده ----------

@Composable
fun FamilyScreen(nav: NavController) {
    val scope = rememberCoroutineScope()
    var tick by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<Member?>(null) }
    Column(Modifier.fillMaxSize()) {
        TopBack("اعضای خانواده", nav)
        Text(
            "اعضا را اینجا ثبت کنید تا هنگام ثبت سفارش بتوانید مشخص کنید سفارش برای چه کسی است.",
            fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(16.dp, 0.dp)
        )
        Button(
            onClick = { editing = Member("", "", "", "") },
            modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp)
        ) { Text("افزودن عضو") }
        Loader(tick, { Api.family() }) { list ->
            if (list.isEmpty()) {
                Empty("هنوز عضوی ثبت نشده است")
            } else {
                LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(list, key = { it.id }) { m ->
                        Card(Modifier.fillMaxWidth(), colors = cardColors()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(m.name + if (m.relation.isNotBlank()) " (${m.relation})" else "", fontWeight = FontWeight.Bold)
                                if (m.note.isNotBlank()) Text(m.note, fontSize = 13.sp)
                                Row {
                                    TextButton(onClick = { editing = m }) { Text("ویرایش") }
                                    TextButton(onClick = {
                                        scope.launch {
                                            withContext(Dispatchers.IO) { runCatching { Api.deleteMember(m.id) } }
                                            tick++
                                        }
                                    }) { Text("حذف", color = Color.Red) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    val e = editing
    if (e != null) MemberDialog(e, onDone = { editing = null; tick++ }, onDismiss = { editing = null })
}

@Composable
fun MemberDialog(m: Member, onDone: () -> Unit, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(m.name) }
    var relation by remember { mutableStateOf(m.relation) }
    var note by remember { mutableStateOf(m.note) }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (m.id.isEmpty()) "عضو جدید" else "ویرایش عضو") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("نام") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(relation, { relation = it }, label = { Text("نسبت (همسر، فرزند، ...)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(note, { note = it }, label = { Text("توضیح کوتاه (اختیاری)") }, modifier = Modifier.fillMaxWidth())
                if (err.isNotEmpty()) Text(err, color = Color.Red)
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = {
                busy = true
                err = ""
                scope.launch {
                    val r = withContext(Dispatchers.IO) { runCatching { Api.saveMember(Member(m.id, name, relation, note)) } }
                    busy = false
                    r.onSuccess { onDone() }.onFailure { err = it.message ?: "خطا" }
                }
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

// ---------- پشتیبانی ----------

@Composable
fun SupportScreen(nav: NavController) {
    var tick by remember { mutableIntStateOf(0) }
    var showNew by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        TopBack("پشتیبانی و مشاوره", nav)
        if (!Store.loggedIn) {
            Empty("برای استفاده از پشتیبانی ابتدا وارد حساب شوید")
        } else {
            Button(onClick = { showNew = true }, modifier = Modifier.fillMaxWidth().padding(16.dp, 4.dp)) { Text("تیکت یا درخواست مشاوره جدید") }
            Loader(tick, { Api.tickets() }) { list ->
                if (list.isEmpty()) {
                    Empty("هنوز گفتگویی ندارید")
                } else {
                    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(list, key = { it.id }) { t ->
                            Card(Modifier.fillMaxWidth().clickable { nav.navigate("ticket/${t.id}") }, colors = cardColors()) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(t.subject, fontWeight = FontWeight.Bold)
                                    Text(
                                        (if (t.category == "consult") "مشاوره" else if (t.category == "feedback") "پیشنهاد و انتقاد" else "پشتیبانی") + " — " + (if (t.status == "closed") "بسته" else "باز"),
                                        fontSize = 12.sp, color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(t.last, fontSize = 13.sp, maxLines = 2)
                                    Text(t.updated, fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (showNew) {
        NewTicketDialog(onDone = { id -> showNew = false; tick++; nav.navigate("ticket/$id") }, onDismiss = { showNew = false })
    }
}

@Composable
fun NewTicketDialog(onDone: (Int) -> Unit, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var category by remember { mutableStateOf("support") }
    var subject by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("گفتگوی جدید") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = category == "support", onClick = { category = "support" }, label = { Text("پشتیبانی") })
                    FilterChip(selected = category == "consult", onClick = { category = "consult" }, label = { Text("مشاوره") })
                    FilterChip(selected = category == "feedback", onClick = { category = "feedback" }, label = { Text("پیشنهاد و انتقاد") })
                }
                OutlinedTextField(subject, { subject = it }, label = { Text("موضوع") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(text, { text = it }, label = { Text("پیام شما") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                if (err.isNotEmpty()) Text(err, color = Color.Red)
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = {
                busy = true
                err = ""
                scope.launch {
                    val r = withContext(Dispatchers.IO) { runCatching { Api.newTicket(category, subject, text) } }
                    busy = false
                    r.onSuccess { onDone(it) }.onFailure { err = it.message ?: "خطا" }
                }
            }) { Text("ارسال") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
fun TicketScreen(nav: NavController, id: Int) {
    var data by remember { mutableStateOf<TicketData?>(null) }
    var text by remember { mutableStateOf("") }
    var err by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(id, refresh) {
        while (true) {
            val r = withContext(Dispatchers.IO) { runCatching { Api.ticket(id) } }
            r.onSuccess { data = it }.onFailure { if (data == null) err = it.message ?: "خطا" }
            delay(10_000)
        }
    }
    val count = data?.messages?.size ?: 0
    LaunchedEffect(count) {
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    Column(Modifier.fillMaxSize()) {
        TopBack(data?.subject ?: "گفتگو", nav)
        Text("پیام‌ها هر ۱۰ ثانیه خودکار به‌روز می‌شوند.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 16.dp))
        if (data == null && err.isNotEmpty()) Text(err, color = Color.Red, modifier = Modifier.padding(16.dp))
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(data?.messages ?: emptyList()) { m ->
                val mine = m.from == "user"
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.Start else Arrangement.End) {
                    Card(
                        Modifier.fillMaxWidth(0.8f),
                        colors = CardDefaults.cardColors(
                            containerColor = if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(Modifier.padding(10.dp)) {
                            Text(if (mine) "شما" else "پشتیبان", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(m.text, fontSize = 14.sp)
                            Text(m.time, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
        if (data?.status == "closed") {
            Text("این گفتگو بسته شده است؛ با ارسال پیام جدید دوباره باز می‌شود.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 16.dp))
        }
        if (err.isNotEmpty() && data != null) Text(err, color = Color.Red, modifier = Modifier.padding(horizontal = 16.dp))
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(text, { text = it }, placeholder = { Text("پیام...") }, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Button(enabled = !busy && text.isNotBlank(), onClick = {
                busy = true
                err = ""
                scope.launch {
                    val r = withContext(Dispatchers.IO) { runCatching { Api.sendMessage(id, text) } }
                    busy = false
                    r.onSuccess { text = ""; refresh++ }.onFailure { err = it.message ?: "خطا" }
                }
            }) { Text("ارسال") }
        }
    }
}

// ---------- سؤال‌های متداول ----------

@Composable
fun FaqScreen(nav: NavController) {
    Column(Modifier.fillMaxSize()) {
        TopBack("سؤال‌های متداول", nav)
        Loader(Unit, { Api.faq() }) { list ->
            if (list.isEmpty()) {
                Empty("هنوز سؤالی ثبت نشده است")
            } else {
                LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(list) { f ->
                        var open by remember { mutableStateOf(false) }
                        Card(Modifier.fillMaxWidth().clickable { open = !open }, colors = cardColors()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(f.q, fontWeight = FontWeight.Bold)
                                if (open) Text(f.a, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------- تنظیمات اعلان ----------

@Composable
fun NotifRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun NotifyScreen(nav: NavController) {
    Column(Modifier.fillMaxSize()) {
        TopBack("تنظیمات اعلان‌ها", nav)
        Column(Modifier.padding(16.dp)) {
            NotifRow("وضعیت سفارش و پاسخ پشتیبانی", Store.notifOrders) { Store.setNotif("orders", it) }
            NotifRow("تخفیف‌ها و پیشنهادهای ویژه", Store.notifPromo) { Store.setNotif("promo", it) }
            NotifRow("موجود شدن کالاهای مورد علاقه", Store.notifStock) { Store.setNotif("stock", it) }
            NotifRow("یادآوری سبد خرید", Store.notifCart) { Store.setNotif("cart", it) }
            Spacer(Modifier.height(12.dp))
            Text(
                "اعلان‌های سفارش و تخفیف فقط وقتی کار می‌کنند که فروشگاه اعلان گوشی را فعال کرده باشد. یادآوری سبد خرید همیشه کار می‌کند.",
                fontSize = 12.sp, color = Color.Gray
            )
        }
    }
}

// ---------- برچسب، شمارش معکوس، فیلتر ----------

@Composable
fun Badge2(text: String, color: Color) {
    Text(
        text, color = Color.White, fontSize = 11.sp,
        modifier = Modifier.background(color, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

@Composable
fun Countdown(seconds: Long) {
    var left by remember(seconds) { mutableLongStateOf(seconds) }
    LaunchedEffect(seconds) {
        while (left > 0) {
            delay(1000)
            left--
        }
    }
    if (left > 0) {
        val d = left / 86400
        val h = (left % 86400) / 3600
        val m = (left % 3600) / 60
        val s = left % 60
        Text(
            "پایان تخفیف: " + (if (d > 0) "$d روز و " else "") + String.format("%02d:%02d:%02d", h, m, s),
            color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold, fontSize = 14.sp
        )
    }
}

@Composable
fun FilterDialog(
    brands: List<Pair<Int, String>>, brand: Int, minP: String, maxP: String,
    onApply: (Int, String, String) -> Unit, onDismiss: () -> Unit
) {
    var b by remember { mutableIntStateOf(brand) }
    var mn by remember { mutableStateOf(minP) }
    var mx by remember { mutableStateOf(maxP) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("فیلتر") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (brands.isNotEmpty()) {
                    Picker(
                        "همه برندها", if (b == 0) "" else b.toString(),
                        listOf("0" to "همه برندها") + brands.map { it.first.toString() to it.second }
                    ) { b = it.toInt() }
                }
                OutlinedTextField(
                    mn, { mn = it }, label = { Text("حداقل قیمت (تومان)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    mx, { mx = it }, label = { Text("حداکثر قیمت (تومان)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { TextButton(onClick = { onApply(b, mn.trim(), mx.trim()) }) { Text("اعمال") } },
        dismissButton = { TextButton(onClick = { onApply(0, "", "") }) { Text("پاک کردن فیلتر") } }
    )
}

// ---------- تاریخ تولد ----------

@Composable
fun BirthdayDialog(onDone: () -> Unit, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val months = listOf("فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور", "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند")
    var month by remember { mutableIntStateOf(1) }
    var day by remember { mutableIntStateOf(1) }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تاریخ تولد (شمسی)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("این تاریخ فقط یک بار قابل ثبت است و بعداً تغییر نمی‌کند.", fontSize = 12.sp, color = Color.Gray)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Picker("ماه", month.toString(), months.mapIndexed { i, n -> (i + 1).toString() to n }) { month = it.toInt() }
                    Picker("روز", day.toString(), (1..31).map { it.toString() to it.toString() }) { day = it.toInt() }
                }
                if (err.isNotEmpty()) Text(err, color = Color.Red)
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = {
                busy = true
                err = ""
                scope.launch {
                    val r = withContext(Dispatchers.IO) { runCatching { Api.setBirthday(String.format("%02d-%02d", month, day)) } }
                    busy = false
                    r.onSuccess { onDone() }.onFailure { err = it.message ?: "خطا" }
                }
            }) { Text("ثبت") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

// ---------- مقایسه ----------

@Composable
fun CompareRow(label: String, a: String, b: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Row {
            Text(a.ifBlank { "—" }, modifier = Modifier.weight(1f), fontSize = 14.sp)
            Text(b.ifBlank { "—" }, modifier = Modifier.weight(1f), fontSize = 14.sp)
        }
        HorizontalDivider()
    }
}

@Composable
fun CompareScreen(nav: NavController) {
    Column(Modifier.fillMaxSize()) {
        TopBack("مقایسه محصولات", nav)
        if (Store.compare.size < 2) {
            Empty("دو محصول انتخاب کنید")
        } else {
            val a = Store.compare[0]
            val b = Store.compare[1]
            Column(Modifier.verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(a, b).forEach { p ->
                        Column(Modifier.weight(1f).clickable { nav.navigate("product/${p.id}") }, horizontalAlignment = Alignment.CenterHorizontally) {
                            AsyncImage(
                                model = p.image, contentDescription = p.name, contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(12.dp))
                            )
                            Text(p.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 2)
                        }
                    }
                }
                CompareRow("قیمت", money(a.finalPrice), money(b.finalPrice))
                CompareRow("برند", a.brand, b.brand)
                CompareRow("دسته", a.categoryName, b.categoryName)
                CompareRow("وزن (گرم)", if (a.weight > 0) a.weight.toString() else "", if (b.weight > 0) b.weight.toString() else "")
                CompareRow("امتیاز کاربران", if (a.ratingCount > 0) "${a.ratingAvg} (${a.ratingCount} نظر)" else "", if (b.ratingCount > 0) "${b.ratingAvg} (${b.ratingCount} نظر)" else "")
                CompareRow("موجودی", if (a.stock > 0) "موجود" else "ناموجود", if (b.stock > 0) "موجود" else "ناموجود")
                CompareRow("تاریخ انقضا", a.expiry, b.expiry)
                CompareRow("ترکیبات", a.ingredients, b.ingredients)
                TextButton(onClick = { Store.compare.clear(); nav.popBackStack() }) { Text("پاک کردن مقایسه") }
            }
        }
    }
}

// ---------- مقالات ----------

@Composable
fun ArticlesScreen(nav: NavController) {
    Column(Modifier.fillMaxSize()) {
        TopBack("مقالات آموزشی", nav)
        Loader(Unit, { Api.articles() }) { list ->
            if (list.isEmpty()) {
                Empty("هنوز مقاله‌ای منتشر نشده است")
            } else {
                LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(list, key = { it.id }) { a ->
                        Card(Modifier.fillMaxWidth().clickable { nav.navigate("article/${a.id}") }, colors = cardColors()) {
                            Column {
                                if (a.image.isNotBlank()) {
                                    AsyncImage(
                                        model = a.image, contentDescription = null, contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxWidth().height(150.dp)
                                    )
                                }
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(a.title, fontWeight = FontWeight.Bold)
                                    Text(a.excerpt, fontSize = 13.sp, maxLines = 3)
                                    Text(a.date, fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArticleScreen(nav: NavController, id: Int) {
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        TopBack("مقاله", nav)
        Loader(id, { Api.article(id) }) { a ->
            Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
                if (a.image.isNotBlank()) {
                    AsyncImage(
                        model = a.image, contentDescription = null, contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(220.dp)
                    )
                }
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(a.title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(a.date, fontSize = 12.sp, color = Color.Gray)
                    Text(a.content, fontSize = 15.sp, lineHeight = 24.sp)
                    if (a.video.isNotBlank()) {
                        Button(onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(a.video))) }, modifier = Modifier.fillMaxWidth()) {
                            Text("مشاهده ویدیو")
                        }
                    }
                }
            }
        }
    }
}

// ---------- درباره ما، قوانین، اینماد و مجوزها ----------

@Composable
fun AboutScreen(nav: NavController) {
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        TopBack("درباره ما و قوانین", nav)
        Loader(Unit, { Pair(Api.pages(), Api.config()) }) { pair ->
            val pages = pair.first
            val cfg = pair.second
            LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (cfg.about.isNotBlank()) {
                    item {
                        Card(Modifier.fillMaxWidth(), colors = cardColors()) { Text(cfg.about, modifier = Modifier.padding(12.dp)) }
                    }
                }
                items(pages) { pg ->
                    var open by remember { mutableStateOf(false) }
                    Card(Modifier.fillMaxWidth().clickable { open = !open }, colors = cardColors()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(pg.title, fontWeight = FontWeight.Bold)
                            if (open) Text(pg.content, fontSize = 14.sp)
                        }
                    }
                }
                if (cfg.storeAddress.isNotBlank() || (cfg.storeLat.isNotBlank() && cfg.storeLng.isNotBlank())) {
                    item {
                        Card(Modifier.fillMaxWidth(), colors = cardColors()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("آدرس فروشگاه", fontWeight = FontWeight.Bold)
                                if (cfg.storeAddress.isNotBlank()) Text(cfg.storeAddress, fontSize = 14.sp)
                                if (cfg.storeLat.isNotBlank() && cfg.storeLng.isNotBlank()) {
                                    OutlinedButton(onClick = {
                                        val geo = "geo:${cfg.storeLat},${cfg.storeLng}?q=${cfg.storeLat},${cfg.storeLng}(${Uri.encode("خانه آرام")})"
                                        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(geo))) }
                                    }) { Text("نمایش روی نقشه") }
                                }
                            }
                        }
                    }
                }
                if (cfg.licenseText.isNotBlank()) {
                    item {
                        Card(Modifier.fillMaxWidth(), colors = cardColors()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("مجوزها", fontWeight = FontWeight.Bold)
                                Text(cfg.licenseText, fontSize = 14.sp)
                            }
                        }
                    }
                }
                if (cfg.instagram.isNotBlank() || cfg.telegram.isNotBlank()) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (cfg.instagram.isNotBlank()) {
                                OutlinedButton(
                                    onClick = { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(cfg.instagram))) } },
                                    modifier = Modifier.weight(1f)
                                ) { Text("اینستاگرام") }
                            }
                            if (cfg.telegram.isNotBlank()) {
                                OutlinedButton(
                                    onClick = { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(cfg.telegram))) } },
                                    modifier = Modifier.weight(1f)
                                ) { Text("کانال تلگرام") }
                            }
                        }
                    }
                }
                if (cfg.enamadUrl.isNotBlank()) {
                    item {
                        OutlinedButton(
                            onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(cfg.enamadUrl))) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("مشاهده نماد اعتماد الکترونیکی (اینماد)") }
                    }
                }
            }
        }
    }
}

// ---------- جستجوی صوتی و اسکن بارکد ----------

@Composable
fun SearchActions(nav: NavController, onVoice: (String) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val voice = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val t = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!t.isNullOrBlank()) onVoice(t)
    }
    val scan = rememberLauncherForActivityResult(ScanContract()) { res ->
        val code = res.contents
        if (!code.isNullOrBlank()) {
            scope.launch {
                val r = withContext(Dispatchers.IO) { runCatching { Api.products(0, "", "newest", 1, 0, "", "", code) } }
                r.onSuccess { page ->
                    if (page.list.isNotEmpty()) {
                        nav.navigate("product/${page.list[0].id}")
                    } else {
                        Toast.makeText(ctx, "محصولی با این بارکد پیدا نشد", Toast.LENGTH_SHORT).show()
                    }
                }.onFailure { Toast.makeText(ctx, it.message ?: "خطا", Toast.LENGTH_SHORT).show() }
            }
        }
    }
    Row {
        TextButton(onClick = {
            val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
                .putExtra(RecognizerIntent.EXTRA_PROMPT, "نام محصول را بگویید")
            try {
                voice.launch(i)
            } catch (e: Exception) {
                Toast.makeText(ctx, "جستجوی صوتی روی این گوشی در دسترس نیست", Toast.LENGTH_SHORT).show()
            }
        }) { Text("🎤") }
        TextButton(onClick = {
            scan.launch(
                ScanOptions()
                    .setPrompt("بارکد محصول را مقابل دوربین بگیرید")
                    .setBeepEnabled(false)
                    .setOrientationLocked(false)
            )
        }) { Text("📷") }
    }
}

// ---------- نظرسنجی ----------

@Composable
fun StarRow(label: String, value: Int, onChange: (Int) -> Unit) {
    Column {
        Text(label, fontSize = 13.sp)
        Row {
            (1..5).forEach { i ->
                TextButton(onClick = { onChange(i) }) { Text(if (i <= value) "★" else "☆", fontSize = 26.sp) }
            }
        }
    }
}

@Composable
fun SurveyDialog(o: OrderRow, onDone: () -> Unit, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var shop by remember { mutableIntStateOf(5) }
    var delivery by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("نظرسنجی سفارش ${o.id}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                StarRow("رضایت شما از فروشگاه و کالا", shop) { shop = it }
                StarRow("رضایت شما از ارسال و پیک", delivery) { delivery = it }
                OutlinedTextField(comment, { comment = it }, label = { Text("توضیحات (اختیاری)") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                if (err.isNotEmpty()) Text(err, color = Color.Red)
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = {
                busy = true
                err = ""
                scope.launch {
                    val r = withContext(Dispatchers.IO) { runCatching { Api.survey(o.id, shop, delivery, comment) } }
                    busy = false
                    r.onSuccess { onDone() }.onFailure { err = it.message ?: "خطا" }
                }
            }) { Text("ثبت") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

// ---------- کیف پول و کارت هدیه ----------

@Composable
fun WalletScreen(nav: NavController) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var tick by remember { mutableIntStateOf(0) }
    var amount by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        TopBack("کیف پول و کارت هدیه", nav)
        if (!Store.loggedIn) {
            Empty("برای استفاده از کیف پول ابتدا وارد حساب شوید")
        } else {
            Loader(tick, { Api.wallet() }) { w ->
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Card(Modifier.fillMaxWidth(), colors = cardColors()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("اعتبار شما")
                                Text(money(w.balance.toLong()), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                TextButton(onClick = { tick++ }) { Text("به‌روزرسانی موجودی") }
                            }
                        }
                    }
                    if (w.online) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("شارژ آنلاین", fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        amount, { amount = it }, label = { Text("مبلغ (تومان)") }, singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Button(enabled = !busy, onClick = {
                                        busy = true
                                        msg = ""
                                        scope.launch {
                                            val r = withContext(Dispatchers.IO) { runCatching { Api.topup(amount) } }
                                            busy = false
                                            r.onSuccess { url ->
                                                if (url.isNotBlank()) {
                                                    msg = "بعد از پرداخت به اپ برگردید و «به‌روزرسانی موجودی» را بزنید."
                                                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                                }
                                            }.onFailure { msg = it.message ?: "خطا" }
                                        }
                                    }) { Text("شارژ") }
                                }
                            }
                        }
                    }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("ثبت کد کارت هدیه", fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(code, { code = it }, label = { Text("کد هدیه") }, singleLine = true, modifier = Modifier.weight(1f))
                                Spacer(Modifier.width(8.dp))
                                Button(enabled = !busy, onClick = {
                                    busy = true
                                    msg = ""
                                    scope.launch {
                                        val r = withContext(Dispatchers.IO) { runCatching { Api.redeem(code.trim()) } }
                                        busy = false
                                        r.onSuccess { a -> msg = "${money(a.toLong())} به کیف پول شما اضافه شد"; code = ""; tick++ }
                                            .onFailure { msg = it.message ?: "خطا" }
                                    }
                                }) { Text("ثبت") }
                            }
                        }
                    }
                    if (msg.isNotEmpty()) item { Text(msg, color = MaterialTheme.colorScheme.primary) }
                    item { Text("تراکنش‌های اخیر", fontWeight = FontWeight.Bold) }
                    if (w.log.isEmpty()) item { Text("هنوز تراکنشی ندارید", color = Color.Gray) }
                    items(w.log) { e ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(e.reason, fontSize = 14.sp)
                                Text(e.date, fontSize = 11.sp, color = Color.Gray)
                            }
                            Text(
                                (if (e.amount > 0) "+" else "") + money(e.amount.toLong()),
                                color = if (e.amount > 0) Color(0xFF2E7D32) else Color(0xFFD32F2F),
                                fontWeight = FontWeight.Bold, fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------- تأیید اصالت کالا ----------

@Composable
fun VerifyScreen(nav: NavController) {
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<VerifyResult?>(null) }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf("") }

    fun check(c: String) {
        busy = true
        err = ""
        result = null
        scope.launch {
            val r = withContext(Dispatchers.IO) { runCatching { Api.verify(c) } }
            busy = false
            r.onSuccess { result = it }.onFailure { err = it.message ?: "خطا" }
        }
    }

    val scan = rememberLauncherForActivityResult(ScanContract()) { res ->
        val c = res.contents
        if (!c.isNullOrBlank()) {
            code = c
            check(c)
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBack("تأیید اصالت کالا", nav)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("کد ۱۰ حرفی روی برچسب یا بسته‌بندی کالا را وارد کنید یا QR روی آن را اسکن کنید.", fontSize = 13.sp, color = Color.Gray)
            OutlinedTextField(code, { code = it }, label = { Text("کد اصالت") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = !busy && code.isNotBlank(), onClick = { check(code.trim()) }) { Text("بررسی") }
                OutlinedButton(onClick = {
                    scan.launch(ScanOptions().setPrompt("کد اصالت را مقابل دوربین بگیرید").setBeepEnabled(false).setOrientationLocked(false))
                }) { Text("اسکن با دوربین") }
            }
            if (busy) CircularProgressIndicator()
            if (err.isNotEmpty()) Text(err, color = Color.Red)
            val r = result
            if (r != null) {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = if (r.ok && r.checks <= 1) Color(0xFFE8F5E9) else if (r.ok) Color(0xFFFFF3E0) else Color(0xFFFFEBEE))
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (r.ok) {
                            Text(r.product, fontWeight = FontWeight.Bold, color = Color.Black)
                            Text("تعداد دفعات بررسی: ${r.checks} — اولین بررسی: ${r.first}", fontSize = 13.sp, color = Color.Black)
                        }
                        Text(r.message, color = Color.Black)
                    }
                }
            }
        }
    }
}

// ---------- قفل اپ ----------

@Composable
fun PinSetDialog(onDone: () -> Unit, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var a by remember { mutableStateOf("") }
    var b by remember { mutableStateOf("") }
    var err by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعیین رمز ۴ رقمی") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    a, { if (it.length <= 4 && it.all { c -> c.isDigit() }) a = it }, label = { Text("رمز") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    b, { if (it.length <= 4 && it.all { c -> c.isDigit() }) b = it }, label = { Text("تکرار رمز") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth()
                )
                if (err.isNotEmpty()) Text(err, color = Color.Red)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (a.length != 4) {
                    err = "رمز باید ۴ رقم باشد"
                } else if (a != b) {
                    err = "دو رمز یکسان نیستند"
                } else {
                    Store.setPin(a)
                    (ctx as? android.app.Activity)?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                    onDone()
                }
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
fun PinCheckDialog(onOk: () -> Unit, onDismiss: () -> Unit) {
    var a by remember { mutableStateOf("") }
    var err by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("رمز فعلی را وارد کنید") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    a, { if (it.length <= 4 && it.all { c -> c.isDigit() }) a = it }, label = { Text("رمز") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth()
                )
                if (err.isNotEmpty()) Text(err, color = Color.Red)
            }
        },
        confirmButton = {
            TextButton(onClick = { if (Store.checkPin(a)) onOk() else err = "رمز اشتباه است" }) { Text("تأیید") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
fun LockSettingsScreen(nav: NavController) {
    val ctx = LocalContext.current
    var showSet by remember { mutableStateOf(false) }
    var showOff by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        TopBack("قفل اپ و اثر انگشت", nav)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "با قفل اپ، هر بار که اپ را باز می‌کنید رمز ۴ رقمی (یا اثر انگشت) لازم است. وقتی قفل فعال باشد، از صفحه اپ عکس صفحه گرفته نمی‌شود و در فهرست برنامه‌های اخیر هم دیده نمی‌شود.",
                fontSize = 13.sp, color = Color.Gray
            )
            if (!Store.lockEnabled) {
                Button(onClick = { showSet = true }, modifier = Modifier.fillMaxWidth()) { Text("فعال‌سازی قفل اپ") }
            } else {
                Text("قفل اپ فعال است", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                if (biometricAvailable(ctx)) {
                    NotifRow("ورود با اثر انگشت", Store.bioEnabled) { Store.setBio(it) }
                } else {
                    Text("اثر انگشت روی این گوشی فعال یا در دسترس نیست.", fontSize = 12.sp, color = Color.Gray)
                }
                OutlinedButton(onClick = { showOff = true }, modifier = Modifier.fillMaxWidth()) { Text("غیرفعال کردن قفل") }
            }
        }
    }
    if (showSet) PinSetDialog(onDone = { showSet = false }, onDismiss = { showSet = false })
    if (showOff) PinCheckDialog(onOk = { Store.clearLock(); showOff = false }, onDismiss = { showOff = false })
}

package com.khanearam.shop

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class ApiError(message: String) : Exception(message)

private fun JSONArray?.objs(): List<JSONObject> {
    if (this == null) return emptyList()
    return (0 until length()).map { getJSONObject(it) }
}

private fun JSONArray?.strs(): List<String> {
    if (this == null) return emptyList()
    return (0 until length()).map { getString(it) }
}

private fun JSONObject.toProduct(): Product = Product(
    id = optInt("id"),
    name = optString("name"),
    price = optLong("price"),
    salePrice = optLong("sale_price"),
    image = optString("image"),
    description = optString("description"),
    stock = optInt("stock"),
    categoryName = optString("category"),
    ratingAvg = optDouble("rating_avg", 0.0),
    ratingCount = optInt("rating_count"),
    images = optJSONArray("images").strs(),
    saleLeft = optLong("sale_left"),
    isNew = optBoolean("is_new"),
    isPopular = optBoolean("is_popular"),
    weight = optInt("weight"),
    brand = optString("brand"),
    expiry = optString("expiry"),
    ingredients = optString("ingredients"),
    related = optJSONArray("related").objs().map { it.toProduct() },
    preorder = optBoolean("preorder"),
    preorderNote = optString("preorder_note")
)

private fun JSONObject.toAddress() = Address(
    optString("id"), optString("title"), optString("name"), optString("phone"),
    optString("city"), optString("address"), optBoolean("default")
)

private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

object Api {
    var token: String = ""

    private fun cacheable(path: String): Boolean {
        val ok = path.startsWith("/home") || path.startsWith("/config") || path.startsWith("/faq") ||
            path.startsWith("/pages") || path.startsWith("/articles") || path.startsWith("/products")
        return ok && (!path.startsWith("/products?") || path.endsWith("search="))
    }

    private fun call(method: String, path: String, body: JSONObject? = null, tokenOverride: String? = null): JSONObject {
        if (BuildConfig.SITE_URL.isBlank()) throw ApiError("آدرس سایت هنگام ساخت اپ وارد نشده است")
        val url = BuildConfig.SITE_URL.trimEnd('/') + "/wp-json/khaneh/v1" + path
        try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = method
            conn.connectTimeout = 15000
            conn.readTimeout = 20000
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("X-App-Key", BuildConfig.APP_KEY)
            val tk = tokenOverride ?: token
            if (tk.isNotEmpty()) conn.setRequestProperty("X-Khaneh-Token", tk)
            if (body != null || method == "POST") {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.outputStream.use { it.write((body ?: JSONObject()).toString().toByteArray(Charsets.UTF_8)) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            val json = try { JSONObject(text) } catch (e: Exception) { JSONObject() }
            if (code !in 200..299) {
                if (code == 401) throw ApiError("لطفاً دوباره وارد حساب خود شوید")
                throw ApiError(json.optString("message", "خطا در ارتباط با سرور"))
            }
            if (method == "GET" && cacheable(path)) Store.cachePut(path, text)
            Store.offline = false
            return json
        } catch (e: IOException) {
            if (method == "GET" && cacheable(path)) {
                val cached = Store.cacheGet(path)
                if (cached != null) {
                    val j = try { JSONObject(cached) } catch (x: Exception) { null }
                    if (j != null) {
                        Store.offline = true
                        return j
                    }
                }
            }
            throw ApiError("اتصال به اینترنت یا سرور برقرار نشد")
        }
    }

    // ---------- فروشگاه ----------

    fun config(): Config {
        val o = call("GET", "/config")
        return Config(
            o.optString("phone"), o.optString("whatsapp"), o.optLong("shipping"), o.optLong("free_from"),
            o.optString("about"), o.optBoolean("online_pay"), o.optLong("point_value"),
            o.optJSONArray("cities").strs(), o.optBoolean("other_cities"), o.optJSONArray("slots").strs(),
            o.optJSONArray("slot_dates").objs().map { SlotDate(it.optString("value"), it.optString("label")) },
            o.optInt("daily_points"),
            o.optJSONArray("brands").objs().map { Pair(it.optInt("id"), it.optString("name")) },
            o.optJSONArray("tiers").objs().map { Pair(it.optLong("min"), it.optInt("pct")) },
            o.optString("enamad_url"), o.optString("license_text"),
            o.optLong("gift_fee"), o.optString("store_address"), o.optString("store_lat"), o.optString("store_lng"),
            o.optString("instagram"), o.optString("telegram"), o.optInt("min_version"), o.optString("update_url")
        )
    }

    fun home(): Home {
        val o = call("GET", "/home")
        return Home(
            banners = o.optJSONArray("banners").objs().map { Banner(it.optString("image"), it.optInt("product_id")) },
            categories = o.optJSONArray("categories").objs().map { Category(it.optInt("id"), it.optString("name"), it.optString("image")) },
            newest = o.optJSONArray("newest").objs().map { it.toProduct() },
            sale = o.optJSONArray("sale").objs().map { it.toProduct() },
            notice = o.optString("notice"),
            popular = o.optJSONArray("popular").objs().map { it.toProduct() }
        )
    }

    fun products(cat: Int, q: String, sort: String, page: Int, brand: Int, min: String, max: String, barcode: String = ""): ProductPage {
        val o = call("GET", "/products?category=$cat&sort=$sort&page=$page&brand=$brand&min=${enc(min)}&max=${enc(max)}&barcode=${enc(barcode)}&search=" + enc(q))
        return ProductPage(o.optJSONArray("products").objs().map { it.toProduct() }, o.optInt("pages", 1))
    }

    fun product(id: Int): Product = call("GET", "/products/$id").getJSONObject("product").toProduct()

    fun notifyStock(productId: Int) {
        call("POST", "/products/$productId/notify")
    }

    fun reviews(productId: Int): ReviewData {
        val o = call("GET", "/products/$productId/reviews")
        return ReviewData(
            o.optDouble("avg", 0.0), o.optInt("count"),
            o.optJSONArray("reviews").objs().map { Review(it.optString("name"), it.optInt("rating"), it.optString("text"), it.optString("date")) }
        )
    }

    fun addReview(productId: Int, rating: Int, text: String): String =
        call("POST", "/products/$productId/reviews", JSONObject().put("rating", rating).put("text", text))
            .optString("message", "نظر ثبت شد")

    fun faq(): List<Faq> = call("GET", "/faq").optJSONArray("faq").objs().map { Faq(it.optString("q"), it.optString("a")) }

    // ---------- حساب ----------

    fun register(name: String, phone: String, password: String, ref: String): Pair<String, String> {
        val o = call("POST", "/register", JSONObject().put("name", name).put("phone", phone).put("password", password).put("ref", ref))
        return Pair(o.getString("token"), o.optString("name"))
    }

    fun login(phone: String, password: String): Pair<String, String> {
        val o = call("POST", "/login", JSONObject().put("phone", phone).put("password", password))
        return Pair(o.getString("token"), o.optString("name"))
    }

    fun me(): Me {
        val o = call("GET", "/me")
        return Me(
            o.optString("name"), o.optString("phone"), o.optInt("points"), o.optString("ref_code"),
            o.optString("level"), o.optInt("level_pct"), o.optString("birthday"), o.optBoolean("daily_claimed"), o.optInt("wallet")
        )
    }

    fun wallet(): WalletInfo {
        val o = call("GET", "/wallet")
        return WalletInfo(
            o.optInt("balance"), o.optBoolean("online"),
            o.optJSONArray("log").objs().map { WalletLog(it.optString("date"), it.optInt("amount"), it.optString("reason")) }
        )
    }

    fun topup(amount: String): String = call("POST", "/wallet/topup", JSONObject().put("amount", amount)).optString("pay_url")

    fun redeem(code: String): Int = call("POST", "/wallet/redeem", JSONObject().put("code", code)).optInt("amount")

    fun verify(code: String): VerifyResult {
        val o = call("GET", "/verify?code=" + enc(code))
        return VerifyResult(o.optBoolean("ok"), o.optString("product"), o.optInt("checks"), o.optString("first"), o.optString("message"))
    }

    fun survey(id: Int, shop: Int, delivery: Int, comment: String) {
        call("POST", "/orders/$id/survey", JSONObject().put("shop_rating", shop).put("delivery_rating", delivery).put("comment", comment))
    }

    fun daily(): Int = call("POST", "/daily").optInt("gained")

    fun setBirthday(md: String) {
        call("POST", "/profile", JSONObject().put("birthday", md))
    }

    fun pages(): List<InfoPage> = call("GET", "/pages").optJSONArray("pages").objs().map { InfoPage(it.optString("title"), it.optString("content")) }

    fun articles(): List<ArticleRow> =
        call("GET", "/articles").optJSONArray("articles").objs().map {
            ArticleRow(it.optInt("id"), it.optString("title"), it.optString("excerpt"), it.optString("image"), it.optString("date"))
        }

    fun article(id: Int): ArticleData {
        val o = call("GET", "/articles/$id")
        return ArticleData(o.optString("title"), o.optString("content"), o.optString("image"), o.optString("video"), o.optString("date"))
    }

    fun deleteAccount() {
        call("DELETE", "/me")
    }

    fun wheel(): WheelInfo {
        val o = call("GET", "/wheel")
        return WheelInfo(o.optJSONArray("labels").strs(), o.optBoolean("spun_today"), o.optInt("points"))
    }

    fun spin(): SpinResult {
        val o = call("POST", "/wheel/spin")
        return SpinResult(o.optString("label"), o.optInt("won"), o.optInt("points"))
    }

    // ---------- آدرس و خانواده ----------

    fun addresses(): List<Address> = call("GET", "/addresses").optJSONArray("addresses").objs().map { it.toAddress() }

    fun saveAddress(a: Address): List<Address> {
        val b = JSONObject().put("id", a.id).put("title", a.title).put("name", a.name).put("phone", a.phone)
            .put("city", a.city).put("address", a.address).put("is_default", a.isDefault)
        return call("POST", "/addresses", b).optJSONArray("addresses").objs().map { it.toAddress() }
    }

    fun deleteAddress(id: String) {
        call("DELETE", "/addresses/$id")
    }

    fun family(): List<Member> =
        call("GET", "/family").optJSONArray("members").objs().map { Member(it.optString("id"), it.optString("name"), it.optString("relation"), it.optString("note")) }

    fun saveMember(m: Member): List<Member> {
        val b = JSONObject().put("id", m.id).put("name", m.name).put("relation", m.relation).put("note", m.note)
        return call("POST", "/family", b).optJSONArray("members").objs().map { Member(it.optString("id"), it.optString("name"), it.optString("relation"), it.optString("note")) }
    }

    fun deleteMember(id: String) {
        call("DELETE", "/family/$id")
    }

    // ---------- ارسال و سفارش ----------

    fun quote(items: List<CartItem>, city: String): List<ShipMethod> {
        val arr = JSONArray()
        items.forEach { arr.put(JSONObject().put("id", it.productId).put("qty", it.qty)) }
        val o = call("POST", "/shipping/quote", JSONObject().put("items", arr).put("city", city))
        return o.optJSONArray("methods").objs().map { ShipMethod(it.optInt("id"), it.optString("title"), it.optLong("cost"), it.optString("type")) }
    }

    fun checkCoupon(code: String): Int = call("GET", "/coupon?code=" + enc(code)).optInt("percent")

    fun createOrder(f: OrderForm): OrderResult {
        val arr = JSONArray()
        f.items.forEach { arr.put(JSONObject().put("id", it.productId).put("qty", it.qty)) }
        val b = JSONObject().put("items", arr).put("name", f.name).put("phone", f.phone)
            .put("address", f.address).put("city", f.city).put("note", f.note).put("coupon", f.coupon)
            .put("pay", if (f.online) "online" else "cod").put("use_points", f.usePoints)
            .put("method", f.method).put("slot_date", f.slotDate).put("slot", f.slot).put("member", f.member)
            .put("use_wallet", f.useWallet).put("gift", f.gift).put("gift_msg", f.giftMsg)
        val o = call("POST", "/orders", b)
        return OrderResult(o.optInt("order_id"), o.optLong("total"), o.optString("pay_url"))
    }

    fun orders(): List<OrderRow> =
        call("GET", "/orders").optJSONArray("orders").objs().map {
            OrderRow(
                it.optInt("id"), it.optString("date"), it.optString("status"), it.optLong("total"), it.optString("items"),
                it.optBoolean("can_pay"), it.optBoolean("can_cancel"),
                it.optJSONArray("lines").objs().map { l -> Pair(l.optInt("id"), l.optInt("qty")) },
                it.optBoolean("can_edit"), it.optBoolean("can_return"), it.optString("return_status"), it.optString("invoice_url"),
                it.optString("name"), it.optString("phone"), it.optString("address"), it.optString("note"),
                it.optString("slot_date"), it.optString("slot"), it.optBoolean("can_survey")
            )
        }

    fun cancelOrder(id: Int) {
        call("POST", "/orders/$id/cancel")
    }

    fun payOrder(id: Int): String = call("POST", "/orders/$id/pay").optString("pay_url")

    fun editOrder(id: Int, name: String, phone: String, address: String, note: String, slotDate: String, slot: String) {
        call("POST", "/orders/$id/edit", JSONObject().put("name", name).put("phone", phone).put("address", address)
            .put("note", note).put("slot_date", slotDate).put("slot", slot))
    }

    fun requestReturn(id: Int, reason: String) {
        call("POST", "/orders/$id/return", JSONObject().put("reason", reason))
    }

    // ---------- پشتیبانی ----------

    fun tickets(): List<TicketRow> =
        call("GET", "/tickets").optJSONArray("tickets").objs().map {
            TicketRow(it.optInt("id"), it.optString("subject"), it.optString("category"), it.optString("status"), it.optString("last"), it.optString("updated"))
        }

    fun newTicket(category: String, subject: String, text: String): Int =
        call("POST", "/tickets", JSONObject().put("category", category).put("subject", subject).put("text", text)).optInt("id")

    fun ticket(id: Int): TicketData {
        val o = call("GET", "/tickets/$id")
        return TicketData(
            o.optString("subject"), o.optString("status"),
            o.optJSONArray("messages").objs().map { TicketMsg(it.optString("from"), it.optString("text"), it.optString("time")) }
        )
    }

    fun sendMessage(id: Int, text: String) {
        call("POST", "/tickets/$id/messages", JSONObject().put("text", text))
    }

    // ---------- اعلان ----------

    fun registerDevice(fcmToken: String, remove: Boolean, tokenOverride: String?) {
        call("POST", "/device", JSONObject().put("token", fcmToken).put("remove", remove), tokenOverride)
    }

    fun getPrefs(): Prefs {
        val o = call("GET", "/prefs")
        return Prefs(o.optBoolean("orders", true), o.optBoolean("promo", true), o.optBoolean("stock", true))
    }

    fun setPrefs(p: Prefs) {
        call("POST", "/prefs", JSONObject().put("orders", p.orders).put("promo", p.promo).put("stock", p.stock))
    }
}

package com.khanearam.shop

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import java.security.MessageDigest
import java.security.SecureRandom
import org.json.JSONObject

object Store {
    private lateinit var sp: SharedPreferences
    private lateinit var app: Context
    private lateinit var cache: SharedPreferences

    val cart = mutableStateListOf<CartItem>()
    val favs = mutableStateListOf<FavItem>()
    val recent = mutableStateListOf<FavItem>()
    val compare = mutableStateListOf<Product>()
    var userName by mutableStateOf("")
    var loggedIn by mutableStateOf(false)
    var query by mutableStateOf("")
    var offline by mutableStateOf(false)
    var lockEnabled by mutableStateOf(false)
    var bioEnabled by mutableStateOf(false)
    var locked by mutableStateOf(false)

    var notifOrders by mutableStateOf(true)
    var notifPromo by mutableStateOf(true)
    var notifStock by mutableStateOf(true)
    var notifCart by mutableStateOf(true)

    fun init(ctx: Context) {
        app = ctx
        sp = ctx.getSharedPreferences("khane_aram", Context.MODE_PRIVATE)
        cache = ctx.getSharedPreferences("khane_cache", Context.MODE_PRIVATE)
        lockEnabled = sp.contains("pin_hash")
        bioEnabled = lockEnabled && sp.getBoolean("bio", false)
        locked = lockEnabled
        Api.token = sp.getString("token", "") ?: ""
        userName = sp.getString("name", "") ?: ""
        loggedIn = Api.token.isNotEmpty()
        notifOrders = sp.getBoolean("n_orders", true)
        notifPromo = sp.getBoolean("n_promo", true)
        notifStock = sp.getBoolean("n_stock", true)
        notifCart = sp.getBoolean("n_cart", true)
        cart.clear()
        favs.clear()
        runCatching {
            val a = JSONArray(sp.getString("cart", "[]"))
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                cart.add(CartItem(o.getInt("id"), o.getString("name"), o.getLong("price"), o.getString("image"), o.getInt("qty")))
            }
        }
        recent.clear()
        runCatching {
            val a = JSONArray(sp.getString("recent", "[]"))
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                recent.add(FavItem(o.getInt("id"), o.getString("name"), o.getLong("price"), o.getString("image")))
            }
        }
        runCatching {
            val a = JSONArray(sp.getString("favs", "[]"))
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                favs.add(FavItem(o.getInt("id"), o.getString("name"), o.getLong("price"), o.getString("image")))
            }
        }
    }

    private fun saveCart() {
        val a = JSONArray()
        cart.forEach { a.put(JSONObject().put("id", it.productId).put("name", it.name).put("price", it.price).put("image", it.image).put("qty", it.qty)) }
        sp.edit().putString("cart", a.toString()).apply()
        Reminder.update(app, cart.isNotEmpty(), notifCart)
    }

    fun addRecent(p: Product) {
        recent.removeAll { it.id == p.id }
        recent.add(0, FavItem(p.id, p.name, p.finalPrice, p.image))
        while (recent.size > 10) recent.removeAt(recent.size - 1)
        val a = JSONArray()
        recent.forEach { a.put(JSONObject().put("id", it.id).put("name", it.name).put("price", it.price).put("image", it.image)) }
        sp.edit().putString("recent", a.toString()).apply()
    }

    fun addCompare(p: Product) {
        if (compare.any { it.id == p.id }) return
        if (compare.size >= 2) compare.removeAt(0)
        compare.add(p)
    }

    private fun saveFavs() {
        val a = JSONArray()
        favs.forEach { a.put(JSONObject().put("id", it.id).put("name", it.name).put("price", it.price).put("image", it.image)) }
        sp.edit().putString("favs", a.toString()).apply()
    }

    fun addToCart(p: Product, qty: Int = 1) {
        val i = cart.indexOfFirst { it.productId == p.id }
        if (i >= 0) {
            cart[i] = cart[i].copy(qty = minOf(20, cart[i].qty + qty))
        } else {
            cart.add(CartItem(p.id, p.name, p.finalPrice, p.image, minOf(20, qty)))
        }
        saveCart()
    }

    fun changeQty(id: Int, delta: Int) {
        val i = cart.indexOfFirst { it.productId == id }
        if (i < 0) return
        val n = cart[i].qty + delta
        if (n <= 0) cart.removeAt(i) else if (n <= 20) cart[i] = cart[i].copy(qty = n)
        saveCart()
    }

    fun removeFromCart(id: Int) {
        cart.removeAll { it.productId == id }
        saveCart()
    }

    fun clearCart() {
        cart.clear()
        saveCart()
    }

    fun cartTotal(): Long {
        var t = 0L
        cart.forEach { t += it.price * it.qty }
        return t
    }

    fun isFav(id: Int): Boolean = favs.any { it.id == id }

    fun toggleFav(p: Product) {
        if (isFav(p.id)) favs.removeAll { it.id == p.id } else favs.add(FavItem(p.id, p.name, p.finalPrice, p.image))
        saveFavs()
    }

    fun removeFav(id: Int) {
        favs.removeAll { it.id == id }
        saveFavs()
    }

    fun setNotif(key: String, value: Boolean) {
        when (key) {
            "orders" -> notifOrders = value
            "promo" -> notifPromo = value
            "stock" -> notifStock = value
            "cart" -> notifCart = value
        }
        sp.edit().putBoolean("n_$key", value).apply()
        if (key == "cart") Reminder.update(app, cart.isNotEmpty(), value)
        if (key == "promo") Push.applyPromoTopic(value)
        if (loggedIn && key != "cart") {
            val p = Prefs(notifOrders, notifPromo, notifStock)
            Thread { runCatching { Api.setPrefs(p) } }.start()
        }
    }

    private fun hashPin(salt: String, pin: String): String {
        val d = MessageDigest.getInstance("SHA-256").digest((salt + pin).toByteArray(Charsets.UTF_8))
        return d.joinToString("") { "%02x".format(it) }
    }

    fun setPin(pin: String) {
        val b = ByteArray(8)
        SecureRandom().nextBytes(b)
        val salt = b.joinToString("") { "%02x".format(it) }
        sp.edit().putString("pin_salt", salt).putString("pin_hash", hashPin(salt, pin)).apply()
        lockEnabled = true
    }

    fun checkPin(pin: String): Boolean {
        val salt = sp.getString("pin_salt", "") ?: ""
        val h = sp.getString("pin_hash", "") ?: ""
        return h.isNotEmpty() && hashPin(salt, pin) == h
    }

    fun setBio(on: Boolean) {
        bioEnabled = on && lockEnabled
        sp.edit().putBoolean("bio", bioEnabled).apply()
    }

    fun clearLock() {
        sp.edit().remove("pin_hash").remove("pin_salt").remove("bio").apply()
        lockEnabled = false
        bioEnabled = false
        locked = false
    }

    @Synchronized
    fun cachePut(path: String, text: String) {
        if (text.length > 400_000) return
        val keys = ArrayList<String>()
        runCatching {
            val a = JSONArray(cache.getString("_keys", "[]"))
            for (i in 0 until a.length()) keys.add(a.getString(i))
        }
        keys.remove(path)
        keys.add(path)
        val ed = cache.edit()
        while (keys.size > 60) ed.remove(keys.removeAt(0))
        ed.putString(path, text).putString("_keys", JSONArray(keys).toString()).apply()
    }

    fun cacheGet(path: String): String? = cache.getString(path, null)

    fun clearCache() {
        cache.edit().clear().apply()
    }

    fun login(token: String, name: String) {
        clearCache()
        Api.token = token
        userName = name
        loggedIn = true
        sp.edit().putString("token", token).putString("name", name).apply()
        val ft = sp.getString("fcm", "") ?: ""
        if (ft.isNotEmpty()) Push.send(ft)
        val p = Prefs(notifOrders, notifPromo, notifStock)
        Thread { runCatching { Api.setPrefs(p) } }.start()
    }

    fun logout() {
        val oldToken = Api.token
        val ft = sp.getString("fcm", "") ?: ""
        if (oldToken.isNotEmpty() && ft.isNotEmpty()) {
            Thread { runCatching { Api.registerDevice(ft, true, oldToken) } }.start()
        }
        Api.token = ""
        userName = ""
        loggedIn = false
        clearCache()
        sp.edit().remove("token").remove("name").apply()
    }
}

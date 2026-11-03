package com.khanearam.shop

import java.text.NumberFormat
import java.util.Locale

data class Category(val id: Int, val name: String, val image: String)
data class Banner(val image: String, val productId: Int)

data class Product(
    val id: Int,
    val name: String,
    val price: Long,
    val salePrice: Long,
    val image: String,
    val description: String,
    val stock: Int,
    val categoryName: String,
    val ratingAvg: Double = 0.0,
    val ratingCount: Int = 0,
    val images: List<String> = emptyList(),
    val saleLeft: Long = 0,
    val isNew: Boolean = false,
    val isPopular: Boolean = false,
    val weight: Int = 0,
    val brand: String = "",
    val expiry: String = "",
    val ingredients: String = "",
    val related: List<Product> = emptyList(),
    val preorder: Boolean = false,
    val preorderNote: String = ""
) {
    val onSale: Boolean get() = salePrice > 0 && salePrice < price
    val finalPrice: Long get() = if (onSale) salePrice else price
}

data class CartItem(val productId: Int, val name: String, val price: Long, val image: String, val qty: Int)
data class FavItem(val id: Int, val name: String, val price: Long, val image: String)

data class OrderRow(
    val id: Int, val date: String, val status: String, val total: Long, val items: String,
    val canPay: Boolean, val canCancel: Boolean, val lines: List<Pair<Int, Int>>,
    val canEdit: Boolean, val canReturn: Boolean, val returnStatus: String, val invoiceUrl: String,
    val name: String, val phone: String, val address: String, val note: String,
    val slotDate: String, val slot: String, val canSurvey: Boolean
)

data class ProductPage(val list: List<Product>, val pages: Int)
data class Me(
    val name: String, val phone: String, val points: Int, val refCode: String,
    val levelName: String, val levelPct: Int, val birthday: String, val dailyClaimed: Boolean, val wallet: Int
)
data class WheelInfo(val labels: List<String>, val spunToday: Boolean, val points: Int)
data class SpinResult(val label: String, val won: Int, val points: Int)
data class Home(val banners: List<Banner>, val categories: List<Category>, val newest: List<Product>, val sale: List<Product>, val notice: String)
data class SlotDate(val value: String, val label: String)

data class Config(
    val phone: String, val whatsapp: String, val shipping: Long, val freeFrom: Long, val about: String,
    val onlinePay: Boolean, val pointValue: Long,
    val cities: List<String>, val otherCities: Boolean, val slots: List<String>, val slotDates: List<SlotDate>,
    val dailyPoints: Int, val brands: List<Pair<Int, String>>, val tiers: List<Pair<Long, Int>>,
    val enamadUrl: String, val licenseText: String,
    val giftFee: Long, val storeAddress: String, val storeLat: String, val storeLng: String,
    val instagram: String, val telegram: String, val minVersion: Int, val updateUrl: String
)

data class ArticleRow(val id: Int, val title: String, val excerpt: String, val image: String, val date: String)
data class ArticleData(val title: String, val content: String, val image: String, val video: String, val date: String)
data class InfoPage(val title: String, val content: String)

data class Review(val name: String, val rating: Int, val text: String, val date: String)
data class ReviewData(val avg: Double, val count: Int, val reviews: List<Review>)
data class OrderResult(val id: Int, val total: Long, val payUrl: String)

data class Address(val id: String, val title: String, val name: String, val phone: String, val city: String, val address: String, val isDefault: Boolean)
data class Member(val id: String, val name: String, val relation: String, val note: String)
data class ShipMethod(val id: Int, val title: String, val cost: Long, val type: String)

data class OrderForm(
    val items: List<CartItem>, val name: String, val phone: String, val address: String, val city: String,
    val note: String, val coupon: String, val online: Boolean, val usePoints: Int,
    val method: Int, val slotDate: String, val slot: String, val member: String,
    val useWallet: Boolean = false, val gift: Boolean = false, val giftMsg: String = ""
)

data class WalletLog(val date: String, val amount: Int, val reason: String)
data class WalletInfo(val balance: Int, val online: Boolean, val log: List<WalletLog>)
data class VerifyResult(val ok: Boolean, val product: String, val checks: Int, val first: String, val message: String)

data class TicketRow(val id: Int, val subject: String, val category: String, val status: String, val last: String, val updated: String)
data class TicketMsg(val from: String, val text: String, val time: String)
data class TicketData(val subject: String, val status: String, val messages: List<TicketMsg>)
data class Faq(val q: String, val a: String)
data class Prefs(val orders: Boolean, val promo: Boolean, val stock: Boolean)

fun money(n: Long): String = NumberFormat.getInstance(Locale.US).format(n) + " تومان"

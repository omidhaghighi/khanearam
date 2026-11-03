package com.khanearam.shop

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : FragmentActivity() {
    private val notifPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private var stoppedAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Store.init(applicationContext)
        Notifier.ensureChannel(this)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        Push.sync(this)
        Push.applyPromoTopic(Store.notifPromo)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                AppTheme { AppRoot() }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (Store.lockEnabled && stoppedAt > 0 && SystemClock.elapsedRealtime() - stoppedAt > 60_000) {
            Store.locked = true
        }
    }

    override fun onResume() {
        super.onResume()
        if (Store.lockEnabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    override fun onStop() {
        super.onStop()
        stoppedAt = SystemClock.elapsedRealtime()
    }
}

@Composable
fun AppRoot() {
    val ctx = LocalContext.current
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    var minVersion by remember { mutableIntStateOf(0) }
    var updateUrl by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val c = withContext(Dispatchers.IO) { runCatching { Api.config() }.getOrNull() }
        if (c != null) {
            minVersion = c.minVersion
            updateUrl = c.updateUrl
        }
    }
    BackHandler(enabled = Store.locked) { }

    val tabs = listOf(
        Triple("home", "خانه", Icons.Filled.Home),
        Triple("favs", "علاقه‌مندی", Icons.Filled.Favorite),
        Triple("cart", "سبد خرید", Icons.Filled.ShoppingCart),
        Triple("account", "حساب من", Icons.Filled.Person)
    )
    Box(Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                if (route in tabs.map { it.first }) {
                    NavigationBar {
                        tabs.forEach { (r, label, icon) ->
                            NavigationBarItem(
                                selected = route == r,
                                onClick = {
                                    nav.navigate(r) {
                                        popUpTo("home")
                                        launchSingleTop = true
                                    }
                                },
                                icon = {
                                    if (r == "cart" && Store.cart.isNotEmpty()) {
                                        BadgedBox(badge = { Badge { Text("${Store.cart.sumOf { it.qty }}") } }) { Icon(icon, null) }
                                    } else {
                                        Icon(icon, null)
                                    }
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        ) { pad ->
            Column(Modifier.padding(pad).consumeWindowInsets(pad).imePadding()) {
                if (Store.offline) {
                    Text(
                        "حالت آفلاین: آخرین اطلاعات ذخیره‌شده نمایش داده می‌شود",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary).padding(6.dp)
                    )
                }
                NavHost(nav, startDestination = "home", modifier = Modifier.weight(1f)) {
                    composable("home") { HomeScreen(nav) }
                    composable("favs") { FavsScreen(nav) }
                    composable("cart") { CartScreen(nav) }
                    composable("account") { AccountScreen(nav) }
                    composable("list/{cat}", arguments = listOf(navArgument("cat") { type = NavType.IntType })) {
                        ListScreen(nav, it.arguments?.getInt("cat") ?: 0)
                    }
                    composable("product/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) {
                        ProductScreen(nav, it.arguments?.getInt("id") ?: 0)
                    }
                    composable("auth") { AuthScreen(nav) }
                    composable("checkout") { CheckoutScreen(nav) }
                    composable("orders") { OrdersScreen(nav) }
                    composable("wheel") { WheelScreen(nav) }
                    composable("addresses") { AddressesScreen(nav) }
                    composable("family") { FamilyScreen(nav) }
                    composable("support") { SupportScreen(nav) }
                    composable("ticket/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) {
                        TicketScreen(nav, it.arguments?.getInt("id") ?: 0)
                    }
                    composable("faq") { FaqScreen(nav) }
                    composable("articles") { ArticlesScreen(nav) }
                    composable("article/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) {
                        ArticleScreen(nav, it.arguments?.getInt("id") ?: 0)
                    }
                    composable("about") { AboutScreen(nav) }
                    composable("compare") { CompareScreen(nav) }
                    composable("notif") { NotifyScreen(nav) }
                    composable("wallet") { WalletScreen(nav) }
                    composable("verify") { VerifyScreen(nav) }
                    composable("lock") { LockSettingsScreen(nav) }
                }
            }
        }
        if (Store.locked) LockScreen()
    }
    if (minVersion > BuildConfig.VERSION_CODE) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("به‌روزرسانی لازم است") },
            text = { Text("برای ادامه استفاده از اپ، نسخه جدید را نصب کنید.") },
            confirmButton = {
                TextButton(onClick = {
                    if (updateUrl.isNotBlank()) ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl)))
                }) { Text("دریافت نسخه جدید") }
            }
        )
    }
}

package com.khanearam.shop

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.concurrent.TimeUnit

object Notifier {
    const val CHANNEL = "khane_default"

    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = ctx.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "اعلان‌های خانه آرام", NotificationManager.IMPORTANCE_DEFAULT))
        }
    }

    fun show(ctx: Context, title: String, body: String) {
        ensureChannel(ctx)
        if (Build.VERSION.SDK_INT >= 33 &&
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val open = Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pi = PendingIntent.getActivity(ctx, 0, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        ctx.getSystemService(NotificationManager::class.java).notify((System.currentTimeMillis() % 100000).toInt(), n)
    }
}

object Push {
    fun onToken(ctx: Context, token: String) {
        ctx.getSharedPreferences("khane_aram", Context.MODE_PRIVATE).edit().putString("fcm", token).apply()
        send(token)
    }

    fun send(token: String) {
        if (Api.token.isNotEmpty()) {
            Thread { runCatching { Api.registerDevice(token, false, null) } }.start()
        }
    }

    fun sync(ctx: Context) {
        runCatching {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { onToken(ctx, it) }
        }
    }

    fun applyPromoTopic(on: Boolean) {
        runCatching {
            val fm = FirebaseMessaging.getInstance()
            if (on) fm.subscribeToTopic("promo") else fm.unsubscribeFromTopic("promo")
        }
    }
}

class PushService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        Push.onToken(applicationContext, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: return
        Notifier.show(applicationContext, title, message.notification?.body ?: message.data["body"] ?: "")
    }
}

object Reminder {
    private const val NAME = "cart_reminder"

    fun update(ctx: Context, cartNotEmpty: Boolean, enabled: Boolean) {
        runCatching {
            val wm = WorkManager.getInstance(ctx)
            if (cartNotEmpty && enabled) {
                wm.enqueueUniqueWork(
                    NAME, ExistingWorkPolicy.REPLACE,
                    OneTimeWorkRequestBuilder<CartWorker>().setInitialDelay(8, TimeUnit.HOURS).build()
                )
            } else {
                wm.cancelUniqueWork(NAME)
            }
        }
    }
}

class CartWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val sp = applicationContext.getSharedPreferences("khane_aram", Context.MODE_PRIVATE)
        val cart = sp.getString("cart", "[]") ?: "[]"
        if (cart != "[]" && sp.getBoolean("n_cart", true)) {
            Notifier.show(applicationContext, "سبد خرید شما منتظر است", "محصولاتی که انتخاب کرده‌اید هنوز در سبد خرید هستند.")
        }
        return Result.success()
    }
}

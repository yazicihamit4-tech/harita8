package com.yazhamit.izmirharita

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d("FCM", "Mesaj Geldi: ${remoteMessage.from}")

        var title: String? = null
        var body: String? = null

        // Firebase Cloud Functions vb. tarafından "notification" veya "data" payload olarak gönderilebilir.
        // İki durumu da yakalayalım.

        // 1. Durum: Gelen mesaj doğrudan "notification" payload içeriyorsa
        remoteMessage.notification?.let {
            title = it.title
            body = it.body
            Log.d("FCM", "Notification Payload: Başlık=${it.title}, Mesaj=${it.body}")
        }

        // 2. Durum: Mesaj "data" payload içeriyorsa (Özellikle custom backend'lerde yaygındır)
        if (remoteMessage.data.isNotEmpty()) {
            Log.d("FCM", "Data Payload: ${remoteMessage.data}")
            if (title == null) title = remoteMessage.data["title"]
            if (body == null) body = remoteMessage.data["body"]

            // Eğer backend farklı bir anahtar kelime ("message" vb.) kullanıyorsa
            if (body == null) body = remoteMessage.data["message"]
        }

        // Başlık ve içeriğimiz hazırsa bildirimi göster
        if (title != null || body != null) {
            sendNotification(title ?: "SİNYAL 35.5", body ?: "Yeni bir bildiriminiz var.")
        } else {
            // Hiçbiri yoksa varsayılan metin göster
            sendNotification("SİNYAL 35.5", "Yeni bir bildirim aldınız.")
        }
    }

    override fun onNewToken(token: String) {
        Log.d("FCM", "Yeni token alındı: $token")
        // Normally send token to server here. However, our application saves it along with new reports.
    }

    private fun sendNotification(title: String, messageBody: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0 /* Request code */, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "sinyal_default_channel"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_round) // Cihazlarda %100 var olan ikon kullanılıyor
            .setContentTitle(title)
            .setContentText(messageBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageBody)) // Uzun metinler için genişletilebilir
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Android 8 altı için yüksek öncelik
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Sinyal Bildirimleri",
                NotificationManager.IMPORTANCE_HIGH // Ekrana düşmesi (heads-up) için IMPORTANCE_HIGH
            )
            channel.description = "Sinyal 35.5 Bildirim Kanalı"
            channel.enableVibration(true)
            notificationManager.createNotificationChannel(channel)
        }

        val notificationId = System.currentTimeMillis().toInt() // Her bildirimi ayrı göstermek için dinamik ID
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}

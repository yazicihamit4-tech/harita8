const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

// 1. KULLANICI YENİ BİR SİNYAL EKLEDİĞİNDE ADMİN'E BİLDİRİM GİTSİN
exports.notifyAdminOnNewSignal = functions.firestore
    .document("sinyaller/{sinyalId}")
    .onCreate(async (snap, context) => {
        const yeniSinyal = snap.data();

        // Push notification payload for Android
        const payload = {
            notification: {
                title: "Yeni İhbar Geldi! 🚨",
                body: `${yeniSinyal.isimSoyisim} yeni bir sorun bildirdi: ${yeniSinyal.aciklama}`
            },
            data: {
                title: "Yeni İhbar Geldi! 🚨",
                body: `${yeniSinyal.isimSoyisim} yeni bir sorun bildirdi: ${yeniSinyal.aciklama}`,
                sinyalId: context.params.sinyalId,
                type: "new_signal"
            }
        };

        // Send notification to the generic "admin_notifications" topic
        try {
            const response = await admin.messaging().sendToTopic("admin_notifications", payload);
            console.log("Adminlere bildirim başarıyla gönderildi:", response);
            return response;
        } catch (error) {
            console.error("Adminlere bildirim gönderilirken hata:", error);
            throw new functions.https.HttpsError("internal", error.message);
        }
    });

// 2. ADMİN BİR SİNYALİ GÜNCELLEDİĞİNDE (CEVAPLADIĞINDA) KULLANICIYA BİLDİRİM GİTSİN
exports.notifyUserOnSignalUpdate = functions.firestore
    .document("sinyaller/{sinyalId}")
    .onUpdate(async (change, context) => {
        const newValue = change.after.data();
        const previousValue = change.before.data();

        // Eğer durum (status) veya admin mesajı değişmişse ve kullanıcının token'ı varsa
        if ((newValue.adminCevap !== previousValue.adminCevap || newValue.durum !== previousValue.durum)
            && newValue.fcmToken) {

            // Push notification payload
            const payload = {
                notification: {
                    title: `Sinyal Durumu Güncellendi: ${newValue.durum}`,
                    body: `Yetkili Yanıtı: ${newValue.adminCevap ? newValue.adminCevap : "İnceleniyor"}`
                },
                data: {
                    title: `Sinyal Durumu Güncellendi: ${newValue.durum}`,
                    body: `Yetkili Yanıtı: ${newValue.adminCevap ? newValue.adminCevap : "İnceleniyor"}`,
                    sinyalId: context.params.sinyalId,
                    type: "signal_update"
                }
            };

            // Doğrudan kullanıcının cihazına (fcmToken üzerinden) bildirim gönderir
            try {
                const response = await admin.messaging().sendToDevice(newValue.fcmToken, payload);
                console.log("Kullanıcıya bildirim başarıyla gönderildi:", response);
                return response;
            } catch (error) {
                console.error("Kullanıcıya bildirim gönderilirken hata:", error);
                throw new functions.https.HttpsError("internal", error.message);
            }
        }
        return null;
    });

package com.example.test_ai_project.core.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.test_ai_project.core.data.R
import com.example.test_ai_project.core.model.Prayer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Posts the notification a prayer alarm exists to produce.
 *
 * Separated from the scheduling side so the two halves fail independently: an alarm that
 * cannot be set exactly still fires, and a notification the user has blocked still leaves
 * the schedule on screen correct.
 *
 * The prayer names live in this module's own resources rather than the feature's. They are
 * duplicated, and deliberately: `:core:data` cannot see `:feature:home`, and a notification
 * that arrives with the app closed has to name the prayer without the UI layer existing.
 */
@Singleton
class PrayerNotifier @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val notificationManager = NotificationManagerCompat.from(context)

    /**
     * Announces that [prayer] has begun.
     *
     * Silently does nothing when notifications are off. That covers both a user who has
     * blocked the channel and an API 33+ device where `POST_NOTIFICATIONS` was never
     * granted — posting in either case is a no-op at best and a `SecurityException` at
     * worst, and neither is worth crashing a background receiver over.
     */
    fun notify(prayer: Prayer) {
        if (!notificationManager.areNotificationsEnabled()) return

        ensureChannel()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_prayer)
            .setContentTitle(context.getString(prayer.nameRes))
            .setContentText(context.getString(R.string.prayer_alert_body, context.getString(prayer.nameRes)))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            // A prayer time is an announcement, not a task: it is true the moment it
            // arrives and needs no action, so tapping it dismisses it.
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(launchIntent())
            .build()

        // One id for every prayer, so a notification left undismissed is replaced by the
        // next one rather than stacking five deep by nightfall.
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Created on demand rather than at startup.
     *
     * Creating a channel that already exists is a documented no-op, so this costs nothing
     * on the second call — and it means a user who never opens the prayer tab never gets a
     * channel in their settings for a feature they have not used.
     */
    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.prayer_channel_name),
            // HIGH, so the alert can make a sound and appear as a heads-up. The whole
            // point is to be noticed at a specific moment; DEFAULT would let it arrive
            // silently in the shade, which is the same as not arriving.
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.prayer_channel_description)
        }

        context.getSystemService(NotificationManager::class.java)
            ?.createNotificationChannel(channel)
    }

    /**
     * Opens the app on tap, without naming an Activity.
     *
     * `:core:data` sits below every feature and must not learn what the launcher Activity
     * is called; asking the package manager keeps that dependency from existing. A null
     * result is possible for an app with no launcher entry, so the notification simply
     * becomes untappable rather than failing to post.
     */
    private fun launchIntent(): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: return null
        return PendingIntent.getActivity(
            context,
            LAUNCH_REQUEST_CODE,
            intent,
            // IMMUTABLE is mandatory from API 31 and correct everywhere: nothing that
            // receives this is meant to rewrite where it points.
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    @get:StringRes
    private val Prayer.nameRes: Int
        get() = when (this) {
            Prayer.Fajr -> R.string.prayer_name_fajr
            Prayer.Dhuhr -> R.string.prayer_name_dhuhr
            Prayer.Asr -> R.string.prayer_name_asr
            Prayer.Maghrib -> R.string.prayer_name_maghrib
            Prayer.Isha -> R.string.prayer_name_isha
        }

    private companion object {
        const val CHANNEL_ID = "prayer_times"
        const val NOTIFICATION_ID = 2001
        const val LAUNCH_REQUEST_CODE = 2002
    }
}

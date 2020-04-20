package com.example.musicplayer

import android.app.*
import android.content.Intent
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat

class MusicPlayerService : Service(), MediaPlayer.OnCompletionListener {

    private val mBinder: IBinder = LocalBinder()
    private lateinit var builder: NotificationCompat.Builder
    private var mNotificationManager: NotificationManager? = null
    private var mServiceHandler: Handler? = null
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var playPauseAction: NotificationCompat.Action
    private lateinit var pausePendingIntent: PendingIntent
    private var playingLocal = false
    private var playingStream = false
    private var started = false


    override fun onCreate() {


        val pauseIntent = Intent(this, MusicPlayerService::class.java)
        pauseIntent.putExtra(PAUSE, true)
        pausePendingIntent = PendingIntent.getService(this,0,pauseIntent,PendingIntent.FLAG_UPDATE_CURRENT)
        playPauseAction = NotificationCompat.Action(R.drawable.ic_pause_circle_outline_black_24dp,"Pause",pausePendingIntent)

        mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = getString(R.string.app_name)
            val mChannel = NotificationChannel(
                CHANNEL_ID,
                name,
                NotificationManager.IMPORTANCE_HIGH
            )
            mChannel.lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            mChannel.setShowBadge(true)
            mNotificationManager!!.createNotificationChannel(mChannel)
        }


    }

    override fun onDestroy() {
        mediaPlayer.release()
        started = false
        setPlayingMusic(this, false)
        super.onDestroy()
    }


    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        val pause = intent.getBooleanExtra(PAUSE,false)
        if (pause) {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
                setPlayingMusic(this,false)
            }
            else {
                mediaPlayer.start()
                setPlayingMusic(this,true)
            }
            mNotificationManager?.notify(NOTIFICATION_ID,updateAndGetNotification())
        }
        Log.i(TAG,"OnStartCommand")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        serviceIsRunningInForeground = false
        stopForeground(true)
        return mBinder
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    override fun onRebind(intent: Intent?) {
        serviceIsRunningInForeground = false
        stopForeground(true)
        super.onRebind(intent)
    }


    override fun onUnbind(intent: Intent?): Boolean {

        serviceIsRunningInForeground = true
        startForeground(NOTIFICATION_ID,updateAndGetNotification())
        return true
    }

    override fun onCompletion(mp: MediaPlayer?) {
        mediaPlayer.release()
        started = false
        setPlayingMusic(this, false)
    }

    fun playPauseAudio() {

        if (!started) return

        if (mediaPlayer.isPlaying) {
            setPlayingMusic(this,false)
            mediaPlayer.pause()
        } else {
            setPlayingMusic(this,true)
            mediaPlayer.start()
        }
    }

    fun playLocal() {
        if (started) {
            mediaPlayer.release()
            mediaPlayer = MediaPlayer.create(this,R.raw.moses_to_you).apply {
                start()
                setOnCompletionListener(this@MusicPlayerService)
            }
        } else {
            started = true
            startService(Intent(this,MusicPlayerService::class.java))
            mediaPlayer = MediaPlayer.create(this,R.raw.moses_to_you).apply {
                start()
                setPlayingMusic(this@MusicPlayerService, true)
                setOnCompletionListener(this@MusicPlayerService)
            }
        }
        playingStream = false
        playingLocal = true
    }

    fun stop() {
        mediaPlayer.stop()
        stopSelf()
    }

    fun playStream(path: Uri) {

        if (started) {

            mediaPlayer.stop()
            mediaPlayer.release()
            mediaPlayer = MediaPlayer().apply {
                setOnPreparedListener {
                    setPlayingMusic(this@MusicPlayerService, true)
                    start()
                }
                setDataSource(path.toString())
                prepareAsync()
            }

        } else {

            startService(Intent(this,MusicPlayerService::class.java))
            started = true
            mediaPlayer = MediaPlayer().apply {
                setOnPreparedListener {
                    setPlayingMusic(this@MusicPlayerService, true)
                    start()
                }
                setDataSource(path.toString())
                prepareAsync()
            }
        }
        playingLocal = false
        playingStream = true

    }

    private fun updateAndGetNotification(): Notification {

        playPauseAction = if (mediaPlayer.isPlaying)
            NotificationCompat.Action(R.drawable.ic_pause_circle_outline_black_24dp,"Pause",pausePendingIntent)
        else
            NotificationCompat.Action(R.drawable.ic_play_circle_outline_black_24dp,"Play",pausePendingIntent)

        val activityPendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT
        )
        builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .addAction(
                R.drawable.ic_play_circle_outline_black_24dp, "Launch activity",
                activityPendingIntent
            )
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    this.resources,
                    R.mipmap.ic_launcher_round
                )
            )
            .setContentText(getNotificationText())
            .setContentTitle("Playing music")
            .setOngoing(true)
            .addAction(playPauseAction)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentIntent(activityPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setTicker(getNotificationText())
            .setOnlyAlertOnce(true)
            .setWhen(System.currentTimeMillis())
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID)
            builder.setOngoing(true)
        }

        return builder.build()
    }

    private fun getNotificationText(): String {
        return if (playingLocal)
            "Moses - to you" else
            "Online radio"
    }

    inner class LocalBinder : Binder() {
        val service: MusicPlayerService
            get() = this@MusicPlayerService
    }

    companion object {
        private const val TAG = "MusicPlayerService"
        private var serviceIsRunningInForeground = false
        private const val NOTIFICATION_ID = 1525
        private const val PAUSE = "pause"
        private val CHANNEL_ID = "${this::class.java.simpleName}.channel"
    }


}
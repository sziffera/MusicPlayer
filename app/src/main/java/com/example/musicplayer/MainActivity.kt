package com.example.musicplayer

import android.content.*
import android.nfc.Tag
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private var musicPlayerService: MusicPlayerService? = null
    private var mBound = false
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            musicPlayerService = null
            mBound = false
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicPlayerService.LocalBinder
            musicPlayerService = binder.service
            musicPlayerService?.loader = loadingPanel
            mBound = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        streamButton.setOnClickListener {
            loadingPanel.visibility = View.VISIBLE
            musicPlayerService?.playStream("http://www.radioideal.net:8026/;stream/1".toUri())
        }


        localMusicButton.setOnClickListener {
            musicPlayerService?.playLocal()
        }

        startPauseButton.setOnClickListener {

            musicPlayerService?.playPauseAudio()

        }
    }

    override fun onStop() {
        if (mBound) {
            unbindService(serviceConnection)
            mBound = false
        }
        PreferenceManager.getDefaultSharedPreferences(this)
            .unregisterOnSharedPreferenceChangeListener(this)
        super.onStop()
    }

    override fun onStart() {

        PreferenceManager.getDefaultSharedPreferences(this)
            .registerOnSharedPreferenceChangeListener(this)

        bindService(
            Intent(applicationContext,MusicPlayerService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
        setButtonState(isPlayingMusic(this))
        super.onStart()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        Log.i(this::class.java.simpleName,"sharedPref changed")
        if (key.equals(KEY_PLAYING_MUSIC)) {
            val isPlaying = sharedPreferences.getBoolean(KEY_PLAYING_MUSIC,false)
            setButtonState(isPlaying)
        }
    }

    private fun setButtonState(isPlaying: Boolean) {
        if (isPlaying) {
            startPauseButton.background = ContextCompat.getDrawable(this,R.drawable.ic_pause_circle_outline_black_24dp)
        } else {
            startPauseButton.background = ContextCompat.getDrawable(this,R.drawable.ic_play_circle_outline_black_24dp)
        }
    }
}

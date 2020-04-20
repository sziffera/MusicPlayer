package com.example.musicplayer

import android.content.Context
import androidx.preference.PreferenceManager

const val KEY_PLAYING_MUSIC = "playingMusic"

fun isPlayingMusic(context: Context): Boolean {
    return PreferenceManager.getDefaultSharedPreferences(context)
        .getBoolean(KEY_PLAYING_MUSIC,false)
}

fun setPlayingMusic(context: Context, playingMusic: Boolean) {
    PreferenceManager.getDefaultSharedPreferences(context)
        .edit()
        .putBoolean(KEY_PLAYING_MUSIC,playingMusic)
        .apply()
}
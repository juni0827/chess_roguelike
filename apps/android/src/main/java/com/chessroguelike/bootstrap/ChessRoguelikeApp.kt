package com.chessroguelike.bootstrap

import android.app.Application
import com.chessroguelike.di.AppContainer

class ChessRoguelikeApp : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}

package com.chessroguelike

import android.app.Application
import com.chessroguelike.app.AppContainer

class ChessRoguelikeApp : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}

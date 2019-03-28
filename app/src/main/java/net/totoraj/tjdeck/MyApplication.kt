package net.totoraj.tjdeck

import android.app.Application
import android.content.Context
import net.totoraj.tjdeck.model.database.MyDatabase
import net.totoraj.tjdeck.repository.TwitterRepository
import twitter4j.TwitterFactory

class MyApplication : Application() {
    companion object {
        private lateinit var appContext: Context
        fun getAppContext(): Context = appContext
    }

    override fun onCreate() {
        super.onCreate()

        appContext = applicationContext
        val twitter = TwitterFactory.getSingleton()

        MyDatabase.createDatabase(appContext)
        TwitterRepository.init(appContext, twitter)

        if (TwitterRepository.Consumer.isSaved) TwitterRepository.Consumer.init()
    }
}
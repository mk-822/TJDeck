package net.totoraj.tjdeck

import android.app.Application
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.totoraj.tjdeck.model.database.MyDatabase
import net.totoraj.tjdeck.model.repository.TwitterRepository
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

        MyDatabase.createDatabase(applicationContext)
        TwitterRepository.init(applicationContext, twitter)

        if (TwitterRepository.Account.isLinked) {
            TwitterRepository.Consumer.init()
            GlobalScope.launch(Dispatchers.IO) {
                TwitterRepository.Token.loadAccessToken()
            }
        }
    }

}
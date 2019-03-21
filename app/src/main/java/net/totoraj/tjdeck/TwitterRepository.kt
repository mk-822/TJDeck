package net.totoraj.tjdeck

import android.content.Context
import android.net.Uri
import android.util.Log
import net.totoraj.tjdeck.model.database.MyDatabase
import net.totoraj.tjdeck.model.database.entity.AccountEntity
import twitter4j.Status
import twitter4j.Twitter
import twitter4j.auth.AccessToken
import twitter4j.auth.RequestToken

class TwitterRepository {
    companion object {
        private lateinit var appContext: Context
        fun setContext(context: Context) {
            appContext = context
        }

        private val db = MyDatabase.getDatabase()

        private lateinit var twitter: Twitter
        fun getTwitter() = twitter
        fun setTwitter(twitter: Twitter) {
            this.twitter = twitter
        }

        fun getString(name: String, key: String, default: String = ""): String =
                appContext.getSharedPreferences(name, Context.MODE_PRIVATE).getString(key, default)!!

        fun setString(name: String, key: String, value: String) =
                appContext.getSharedPreferences(name, Context.MODE_PRIVATE).edit().putString(key, value).apply()

        fun getBoolean(name: String, key: String, default: Boolean = false): Boolean =
                appContext.getSharedPreferences(name, Context.MODE_PRIVATE).getBoolean(key, default)

        fun setBoolean(name: String, key: String, value: Boolean) =
                appContext.getSharedPreferences(name, Context.MODE_PRIVATE).edit().putBoolean(key, value).apply()

        suspend fun tweet(s: String, callback: (exception: Exception?) -> Unit) {
            val exception = try {
                twitter.updateStatus(s)
                null
            } catch (e: Exception) {
                Log.e("onTweet", "error occurred", e)
                e
            }
            callback.invoke(exception)
        }

    }

    class Consumer {
        companion object {
            private const val prefName = "Consumer"
            private const val keyIsRegistered = "isRegistered"
            private const val keyConsumerKey = "consumerKey"
            private const val keyConsumerSecret = "consumerSecret"

            var isRegistered: Boolean
                get() = getBoolean(prefName, keyIsRegistered)
                set(value) = setBoolean(prefName, keyIsRegistered, value)

            var key: String = getString(prefName, keyConsumerKey)
            var secret: String = getString(prefName, keyConsumerSecret)

            fun init() {
                twitter.setOAuthConsumer(Consumer.key, Consumer.secret)
            }

            fun save() {
                setString(prefName, keyConsumerKey, key)
                setString(prefName, keyConsumerSecret, secret)
                isRegistered = true
            }
        }
    }

    class Token {
        companion object {
            private lateinit var requestToken: RequestToken

            suspend fun getRequestToken(callback: (urlString: String) -> Unit) {
                if (!Consumer.isRegistered) Consumer.init()
                twitter.oAuthAccessToken = null

                val urlString = try {
                    requestToken = twitter.oAuthRequestToken
                    requestToken.authorizationURL
                } catch (e: Exception) {
                    Log.e("onGetRequestToken", "error occurred", e)
                    ""
                }

                callback.invoke(urlString)
            }

            suspend fun getAccessToken(pin: String, callback: (hasToken: Boolean) -> Unit) {
                val hasToken = try {
                    val accessToken = twitter.getOAuthAccessToken(requestToken, pin)
                    Account.add(accessToken)
                    if (!Consumer.isRegistered) Consumer.save()
                    true
                } catch (e: Exception) {
                    Log.e("onGetAccessToken", "error occurred", e)
                    false
                }

                callback.invoke(hasToken)
            }

            suspend fun loadAccessToken() {
                twitter.oAuthAccessToken = Account.getDefault()?.let { AccessToken(it.token, it.tokenSecret) }
            }
        }
    }

    class Account {
        companion object {
            private const val prefName = "Account"
            private const val keyIsLinked = "isLinked"

            var isLinked: Boolean
                get() = getBoolean(prefName, keyIsLinked)
                set(value) = setBoolean(prefName, keyIsLinked, value)

            suspend fun getAll() = db.accountDao.findAll()

            suspend fun getDefault(): AccountEntity? {
                return try {
                    db.accountDao.findDefaultAccount().first()
                } catch (e: Exception) {
                    null
                }
            }

            suspend fun setDefault(userId: String) {
                setDefault(db.accountDao.findByUserId(userId).first().apply { isDefaultUser = true })
            }

            private suspend fun setDefault(newDefaultAccount: AccountEntity) {
                newDefaultAccount.isDefaultUser = true
                val prevDefaultAccount = getDefault()?.apply { isDefaultUser = false }
                upsert(newDefaultAccount, prevDefaultAccount)
            }

            suspend fun add(accessToken: AccessToken) {
                val userId = accessToken.userId
                val newAccount = AccountEntity(userId).apply {
                    token = accessToken.token
                    tokenSecret = accessToken.tokenSecret
                    iconUrl = twitter.showUser(userId).miniProfileImageURLHttps
                }

                isLinked = true
                setDefault(newAccount)
            }

            private suspend fun upsert(newAccount: AccountEntity, prevDefaultAccount: AccountEntity?) {
                db.beginTransaction()
                try {
                    prevDefaultAccount?.let { db.accountDao.upsert(listOf(it)) }
                    db.accountDao.upsert(listOf(newAccount))
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            }
        }
    }

}
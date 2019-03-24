package net.totoraj.tjdeck.model.repository

import android.content.Context
import android.net.Uri
import android.text.format.DateFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.totoraj.tjdeck.R
import net.totoraj.tjdeck.model.database.MyDatabase
import net.totoraj.tjdeck.model.database.entity.AccountEntity
import twitter4j.StatusUpdate
import twitter4j.Twitter
import twitter4j.auth.AccessToken
import twitter4j.auth.RequestToken
import java.util.*

class TwitterRepository {
    companion object {
        private const val dateFormat = "kkmmss"

        private lateinit var appContext: Context
        private lateinit var twitter: Twitter
        private lateinit var db: MyDatabase
        private var maxMediaWithTweet: Int = 0

        fun init(context: Context, twitter: Twitter) {
            appContext = context
            Companion.twitter = twitter
            db = MyDatabase.getDatabase()
            maxMediaWithTweet = appContext.resources.getInteger(R.integer.max_media_with_tweet)
        }

        fun getString(name: String, key: String, default: String = ""): String =
                appContext.getSharedPreferences(name, Context.MODE_PRIVATE).getString(key, default)!!

        fun setString(name: String, key: String, value: String) =
                appContext.getSharedPreferences(name, Context.MODE_PRIVATE).edit().putString(key, value).apply()

        fun getBoolean(name: String, key: String, default: Boolean = false): Boolean =
                appContext.getSharedPreferences(name, Context.MODE_PRIVATE).getBoolean(key, default)

        fun setBoolean(name: String, key: String, value: Boolean) =
                appContext.getSharedPreferences(name, Context.MODE_PRIVATE).edit().putBoolean(key, value).apply()

        fun getMaxMediaWithTweet() = maxMediaWithTweet

        suspend fun tweet(tweet: String, uriList: List<Uri>) = withContext(Dispatchers.IO) {
            try {
                val status = StatusUpdate(tweet)
                if (uriList.isNotEmpty()) {
                    val mediaIds = mutableListOf<Long>()
                    val prefix = DateFormat.format(dateFormat, Calendar.getInstance()).toString() + "_"

                    uriList.forEachIndexed { index, uri ->
                        val inputStream = appContext.contentResolver.openInputStream(uri)
                        mediaIds.add(twitter.uploadMedia("$prefix$index", inputStream).mediaId)
                    }

                    status.setMediaIds(*mediaIds.toLongArray())
                }

                twitter.updateStatus(status)
                Result.success("tweet: $tweet")
            } catch (e: Exception) {
                Result.failure<Exception>(e)
            }
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
                twitter.setOAuthConsumer(key, secret)
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

            suspend fun getRequestToken() = withContext(Dispatchers.IO) {
                try {
                    if (!Consumer.isRegistered) Consumer.init()
                    twitter.oAuthAccessToken = null
                    requestToken = twitter.oAuthRequestToken
                    Result.success(requestToken.authorizationURL!!)
                } catch (e: Exception) {
                    Result.failure<Exception>(e)
                }
            }

            suspend fun getAccessToken(pin: String) = withContext(Dispatchers.IO) {
                try {
                    val accessToken = twitter.getOAuthAccessToken(requestToken, pin)
                    Account.add(accessToken)
                    if (!Consumer.isRegistered) Consumer.save()
                    Result.success(accessToken)
                } catch (e: Exception) {
                    Result.failure<Exception>(e)
                }
            }

            suspend fun loadAccessToken() = withContext(Dispatchers.IO) {
                try {
                    val accessToken = Account.getDefault()?.let { AccessToken(it.token, it.tokenSecret) }
                    twitter.oAuthAccessToken = accessToken
                    Result.success(accessToken)
                } catch (e: Exception) {
                    Result.failure<Exception>(e)
                }
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

            suspend fun getAll() = withContext(Dispatchers.IO) {
                try {
                    val accounts = db.accountDao.findAll()
                    Result.success(accounts)
                } catch (e: Exception) {
                    Result.failure<Exception>(e)
                }
            }

            suspend fun getDefault(): AccountEntity? = withContext(Dispatchers.IO) {
                try {
                    db.accountDao.findDefaultAccount().first()
                } catch (e: Exception) {
                    null
                }
            }

            suspend fun setDefault(userId: String) = withContext(Dispatchers.IO) {
                setDefault(db.accountDao.findByUserId(userId).first().apply { isDefaultUser = true })
            }

            private suspend fun setDefault(newDefaultAccount: AccountEntity) = withContext(Dispatchers.IO) {
                newDefaultAccount.isDefaultUser = true
                val prevDefaultAccount = getDefault()?.apply { isDefaultUser = false }
                upsert(newDefaultAccount, prevDefaultAccount)
            }

            suspend fun add(accessToken: AccessToken) = withContext(Dispatchers.IO) {
                val userId = accessToken.userId
                val newAccount = AccountEntity(userId).apply {
                    token = accessToken.token
                    tokenSecret = accessToken.tokenSecret
                    iconUrl = twitter.showUser(userId).miniProfileImageURLHttps
                }

                isLinked = true
                setDefault(newAccount)
            }

            private suspend fun upsert(newAccount: AccountEntity, prevDefaultAccount: AccountEntity?) =
                    withContext(Dispatchers.IO) {
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

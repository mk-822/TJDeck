package net.totoraj.tjdeck.repository

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
import twitter4j.TwitterException
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

        fun setAccessToken(account: AccountEntity?) {
            if (!TwitterRepository.Consumer.isSaved) return
            twitter.oAuthAccessToken = account?.run { AccessToken(token, tokenSecret) }
        }

        suspend fun tweet(tweet: String, uriList: List<Uri>) = withContext(Dispatchers.IO) {
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
            return@withContext "tweet: $tweet"
        }
    }

    class Consumer {
        companion object {
            private const val prefName = "Consumer"
            private const val keyIsSaved = "isSaved"
            private const val keyConsumerKey = "consumerKey"
            private const val keyConsumerSecret = "consumerSecret"

            var isSaved: Boolean
                get() = getBoolean(prefName, keyIsSaved)
                set(value) = setBoolean(prefName, keyIsSaved, value)

            var key: String = getString(prefName, keyConsumerKey)
            var secret: String = getString(prefName, keyConsumerSecret)

            fun init() {
                twitter.setOAuthConsumer(key, secret)
            }

            fun save() {
                setString(prefName, keyConsumerKey, key)
                setString(prefName, keyConsumerSecret, secret)
                isSaved = true
            }
        }
    }

    class Token {
        companion object {
            private lateinit var requestToken: RequestToken

            suspend fun getRequestToken() = withContext(Dispatchers.IO) {
                if (!Consumer.isSaved) Consumer.init()
                setAccessToken(null)
                requestToken = twitter.oAuthRequestToken
                return@withContext requestToken.authorizationURL!!
            }

            suspend fun getAccessToken(pin: String) = withContext(Dispatchers.IO) {
                val accessToken = twitter.getOAuthAccessToken(requestToken, pin)
                val userId = accessToken.userId
                val newAccount = AccountEntity(userId).apply {
                    token = accessToken.token
                    tokenSecret = accessToken.tokenSecret
                    iconUrl = twitter.showUser(userId).profileImageURLHttps
                    isDefaultUser = true
                }
                if (!Consumer.isSaved) Consumer.save()
                return@withContext newAccount
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

            suspend fun findAll() = withContext(Dispatchers.IO) {
                try {
                    db.beginTransaction()
                    val accounts = db.accountDao.findAll()
                    db.setTransactionSuccessful()
                    return@withContext accounts
                } finally {
                    db.endTransaction()
                }
            }

            suspend fun verify() = withContext(Dispatchers.IO) {
                val accounts = findAll()
                val validAccounts = accounts.toMutableList()
                val invalidAccounts = mutableListOf<AccountEntity>()

                accounts.forEach {
                    try {
                        setAccessToken(it)
                        twitter.verifyCredentials()
                    } catch (e: TwitterException) {
                        when (e.errorCode) {
                            220, 401, 403 -> {
                                validAccounts.remove(it)
                                invalidAccounts.add(it)
                            }
                            else -> throw e
                        }
                    }
                }
                return@withContext Pair(validAccounts, invalidAccounts)
            }

            suspend fun upsert(accounts: List<AccountEntity>) = withContext(Dispatchers.IO) {
                try {
                    db.beginTransaction()
                    db.accountDao.upsert(accounts)
                    db.setTransactionSuccessful()
                    return@withContext accounts
                } finally {
                    db.endTransaction()
                }
            }

            suspend fun delete(accounts: List<AccountEntity>) = withContext(Dispatchers.IO) {
                try {
                    db.beginTransaction()
                    db.accountDao.delete(accounts)
                    db.setTransactionSuccessful()
                    return@withContext accounts
                } finally {
                    db.endTransaction()
                }
            }

            suspend fun deleteAll() = withContext(Dispatchers.IO) {
                try {
                    db.beginTransaction()
                    db.accountDao.deleteAll()
                    isLinked = false
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            }
        }
    }
}


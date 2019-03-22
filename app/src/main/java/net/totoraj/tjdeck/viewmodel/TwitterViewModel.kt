package net.totoraj.tjdeck.viewmodel

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import kotlinx.coroutines.*
import net.totoraj.tjdeck.model.database.entity.AccountEntity
import net.totoraj.tjdeck.model.exception.AccessTokenException
import net.totoraj.tjdeck.model.exception.RequestTokenException
import net.totoraj.tjdeck.model.repository.TwitterRepository
import twitter4j.TwitterException
import twitter4j.auth.AccessToken
import kotlin.coroutines.CoroutineContext

class TwitterViewModel : ViewModel(), CoroutineScope {
    companion object {
        fun getModel(owner: FragmentActivity) = ViewModelProviders.of(owner).get(TwitterViewModel::class.java)
    }

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private val throwable = MutableLiveData<Pair<String, Throwable>>()
    private val urlString = MutableLiveData<String>()
    private val accessToken = MutableLiveData<AccessToken>()
    private val accounts = MutableLiveData<List<AccountEntity>>()

    override fun onCleared() {
        super.onCleared()
        coroutineContext.cancelChildren()

    }

    fun observeThrowable(owner: FragmentActivity, callee: (data: Pair<String, Throwable>?) -> Unit) =
            throwable.observe(owner, Observer<Pair<String, Throwable>> { data -> callee.invoke(data) })

    fun observeThrowable(owner: Fragment, callee: (data: Pair<String, Throwable>?) -> Unit) =
            throwable.observe(owner, Observer<Pair<String, Throwable>> { data -> callee.invoke(data) })

    fun observeCallbackUrl(owner: FragmentActivity, callee: (data: String?) -> Unit) =
            urlString.observe(owner, Observer<String> { data -> callee.invoke(data) })

    fun observeCallbackUrl(owner: Fragment, callee: (data: String?) -> Unit) =
            urlString.observe(owner, Observer<String> { data -> callee.invoke(data) })

    fun observeAccessToken(owner: FragmentActivity, callee: (data: AccessToken?) -> Unit) =
            accessToken.observe(owner, Observer<AccessToken> { data -> callee.invoke(data) })

    fun observeAccessToken(owner: Fragment, callee: (data: AccessToken?) -> Unit) =
            accessToken.observe(owner, Observer<AccessToken> { data -> callee.invoke(data) })

    fun observeLinkedAccounts(owner: FragmentActivity, callee: (data: List<AccountEntity>?) -> Unit) =
            accounts.observe(owner, Observer<List<AccountEntity>> { data -> callee.invoke(data) })

    fun observeLinkedAccounts(owner: Fragment, callee: (data: List<AccountEntity>?) -> Unit) =
            accounts.observe(owner, Observer<List<AccountEntity>> { data -> callee.invoke(data) })

    fun getRequestToken(consumerKey: String, consumerSecret: String) {
        launch {
            TwitterRepository.Consumer.key = consumerKey
            TwitterRepository.Consumer.secret = consumerSecret
            TwitterRepository.Token.getRequestToken().fold(
                    onSuccess = { urlString.postValue(it as String) },
                    onFailure = {
                        throws(when (it) {
                            is TwitterException -> RequestTokenException(it)
                            else -> it
                        })
                    }
            )
        }
    }

    fun getAccessToken(pin: String) {
        launch {
            TwitterRepository.Token.getAccessToken(pin).fold(
                    onSuccess = { accessToken.postValue(it as AccessToken) },
                    onFailure = {
                        throws(when (it) {
                            is TwitterException -> AccessTokenException(it)
                            else -> it
                        })
                    }
            )
        }
    }

    fun loadAccessToken() {
        launch {
            TwitterRepository.Token.loadAccessToken().fold(
                    onSuccess = {
                        it?.run {
                            this as AccessToken
                            Log.d("onLoadAccessToken", "load: ${this.screenName}")
                        } ?: Log.d("onLoadAccessToken", "linked account is nothing")
                    },
                    onFailure = { throws(it) }
            )
        }
    }

    fun refreshLinkedAccounts() {
        launch {
            TwitterRepository.Account.getAll().fold(
                    onSuccess = {
                        @Suppress("UNCHECKED_CAST")
                        accounts.postValue(it as List<AccountEntity>)
                    },
                    onFailure = { throws(it) }
            )
        }
    }

    fun tweet(s: String) {
        launch {
            TwitterRepository.tweet(s).fold(
                    onSuccess = {},
                    onFailure = { throws(it) }
            )
        }
    }

    private fun throws(cause: Throwable) {
        val errorMessage = when (cause) {
            is TwitterException -> cause.errorMessage ?: "network error occurred"
            else -> "unexpected error occurred"
        }

        throwable.postValue(Pair(errorMessage, cause))
    }
}

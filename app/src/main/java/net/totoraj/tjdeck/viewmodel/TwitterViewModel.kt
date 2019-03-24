package net.totoraj.tjdeck.viewmodel

import android.net.Uri
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import kotlinx.coroutines.*
import net.totoraj.tjdeck.exception.AccessTokenException
import net.totoraj.tjdeck.exception.RequestTokenException
import net.totoraj.tjdeck.exception.UploadFilesLimitException
import net.totoraj.tjdeck.model.database.entity.AccountEntity
import net.totoraj.tjdeck.model.repository.TwitterRepository
import twitter4j.TwitterException
import twitter4j.auth.AccessToken
import kotlin.coroutines.CoroutineContext

class TwitterViewModel : ViewModel(), CoroutineScope {
    companion object {
        fun getModel(owner: FragmentActivity) =
                ViewModelProviders.of(owner).get(TwitterViewModel::class.java)
    }

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job
    private val maxMediaWithTweet = TwitterRepository.getMaxMediaWithTweet()

    private val throwable = MutableLiveData<Pair<String, Throwable>>()
    private val urlString = MutableLiveData<String>()
    private val accessToken = MutableLiveData<AccessToken>()
    private val accounts = MutableLiveData<List<AccountEntity>>()
    private val files = MutableLiveData<List<Uri>>()

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

    fun observeFiles(owner: FragmentActivity, callee: (data: List<Uri>?) -> Unit) =
            files.observe(owner, Observer<List<Uri>> { data -> callee.invoke(data) })

    fun observeFiles(owner: Fragment, callee: (data: List<Uri>?) -> Unit) =
            files.observe(owner, Observer<List<Uri>> { data -> callee.invoke(data) })

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
        val urlList = files.value ?: listOf()
        removeAllFile()
        launch {
            TwitterRepository.tweet(s, urlList).fold(
                    onSuccess = { removeAllFile() },
                    onFailure = { throws(it) }
            )
        }
    }

    fun addFiles(files: MutableList<Uri>) {
        val newFiles = this.files.value?.toMutableList() ?: mutableListOf()
        while (newFiles.size < maxMediaWithTweet) {
            if (files.isEmpty()) break
            newFiles.add(files.removeAt(0))
        }

        if (files.size > 0) throws(UploadFilesLimitException("Up to $maxMediaWithTweet files"))
        this.files.postValue(newFiles)
    }

    fun removeAllFile() {
        files.postValue(listOf())
    }

    fun removeFile(index: Int) {
        val newFiles = files.value?.toMutableList() ?: return
        newFiles.removeAt(index)
        files.postValue(newFiles)
    }

    private fun throws(cause: Throwable) {
        val errorMessage = when (cause) {
            is TwitterException -> cause.errorMessage ?: "Network error occurred"
            else -> cause.message ?: "Unexpected error occurred"
        }

        throwable.postValue(Pair(errorMessage, cause))
    }
}

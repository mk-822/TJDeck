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
import net.totoraj.tjdeck.exception.InvalidAccountException
import net.totoraj.tjdeck.exception.RequestTokenException
import net.totoraj.tjdeck.exception.UploadFilesLimitException
import net.totoraj.tjdeck.model.database.entity.AccountEntity
import net.totoraj.tjdeck.repository.TwitterRepository
import twitter4j.TwitterException
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

    fun observeLinkedAccounts(owner: FragmentActivity, callee: (data: List<AccountEntity>?) -> Unit) =
            accounts.observe(owner, Observer<List<AccountEntity>> { data -> callee.invoke(data) })

    fun observeLinkedAccounts(owner: Fragment, callee: (data: List<AccountEntity>?) -> Unit) =
            accounts.observe(owner, Observer<List<AccountEntity>> { data -> callee.invoke(data) })

    fun observeFiles(owner: FragmentActivity, callee: (data: List<Uri>?) -> Unit) =
            files.observe(owner, Observer<List<Uri>> { data -> callee.invoke(data) })

    fun observeFiles(owner: Fragment, callee: (data: List<Uri>?) -> Unit) =
            files.observe(owner, Observer<List<Uri>> { data -> callee.invoke(data) })

    fun getRequestToken(consumerKey: String, consumerSecret: String) = launch {
        TwitterRepository.Consumer.key = consumerKey
        TwitterRepository.Consumer.secret = consumerSecret
        runCatching { TwitterRepository.Token.getRequestToken() }.fold(
                onSuccess = { urlString.postValue(it) },
                onFailure = {
                    throws(when (it) {
                        is TwitterException -> RequestTokenException(it)
                        else -> it
                    })
                }
        )
    }


    fun getAccessToken(pin: String) = launch {
        runCatching { TwitterRepository.Token.getAccessToken(pin) }.fold(
                onSuccess = { newAccount ->
                    setDefaultAccount(newAccount)
                    TwitterRepository.Account.isLinked = true
                },
                onFailure = {
                    restoreDefaultAccount()
                    throws(when (it) {
                        is TwitterException -> AccessTokenException(it)
                        else -> it
                    })
                }
        )
    }

    fun verifyAccounts() = launch {
        runCatching { TwitterRepository.Account.verify() }.fold(
                onSuccess = { result ->
                    val (validAccounts, invalidAccounts) = result

                    if (validAccounts.isEmpty()) {
                        TwitterRepository.Account.isLinked = false
                        TwitterRepository.setAccessToken(null)
                        accounts.postValue(listOf())
                        return@fold
                    }

                    if (invalidAccounts.any { it.isDefaultUser }) {
                        validAccounts.first().run { isDefaultUser = true }
                    }

                    validAccounts.find { it.isDefaultUser }.let {
                        TwitterRepository.setAccessToken(it)
                    }

                    accounts.postValue(validAccounts)

                    if (invalidAccounts.isNotEmpty()) throws(
                            InvalidAccountException("Unlink invalid account")
                    )
                },
                onFailure = { throws(it) }
        )
    }

    fun setDefaultAccount(newAccount: AccountEntity) {
        TwitterRepository.setAccessToken(newAccount)

        val new = accounts.value?.toMutableList() ?: mutableListOf()
        if (!new.contains(newAccount)) new.add(newAccount)

        new.replaceAll {
            it.clone().apply { isDefaultUser = userId == newAccount.userId }
        }
        accounts.postValue(new)
    }

    private fun restoreDefaultAccount() {
        (accounts.value?.toMutableList() ?: mutableListOf()).run {
            find { it.isDefaultUser }.let { TwitterRepository.setAccessToken(it) }
        }
    }

    fun storeAccounts() = runBlocking {
        val toStoreAccounts = accounts.value ?: return@runBlocking
        if (toStoreAccounts.isEmpty()) {
            TwitterRepository.Account.deleteAll()
            return@runBlocking
        }

        runCatching { TwitterRepository.Account.findAll() }.fold(
                onSuccess = { result ->
                    val toDeleteAccounts = result.toMutableList()
                    toDeleteAccounts.retainAll { !toStoreAccounts.contains(it) }

                    TwitterRepository.Account.upsert(toStoreAccounts)
                    TwitterRepository.Account.delete(toDeleteAccounts)
                    return@runBlocking
                },
                onFailure = {
                    Log.e("TwitterViewModel", "error occurred in storeAccounts", it)
                    return@runBlocking
                }
        )
    }

    fun tweet(s: String) = launch {
        val uriList = files.value ?: listOf()
        files.postValue(listOf())
        runCatching { TwitterRepository.tweet(s, uriList) }.fold(
                onSuccess = {},
                onFailure = { throws(it) }
        )
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

    // for debug
    fun addDummyAccounts(size: Int = 10) {
        val new = accounts.value?.toMutableList() ?: mutableListOf()
        repeat(size) {
            new.add(AccountEntity(it.toLong()).apply {
                token = ""
                tokenSecret = ""
                iconUrl = "https://google.com"
            })
        }
        accounts.postValue(new)
    }
}

package net.totoraj.tjdeck

import android.media.audiofx.BassBoost
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import android.net.Uri
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.totoraj.tjdeck.model.database.entity.AccountEntity
import twitter4j.auth.AccessToken
import twitter4j.auth.RequestToken

class AccountLinkageSettingsViewModel : ViewModel() {
    companion object {
        fun getModel(owner: FragmentActivity) = ViewModelProviders.of(owner).get(AccountLinkageSettingsViewModel::class.java)
    }

    private val callbackUrl = MutableLiveData<String>()
    private val hasToken = MutableLiveData<Boolean>()
    private val accounts = MutableLiveData<List<AccountEntity>>()

    fun observeCallbackUrl(owner: FragmentActivity, callback: (data: String?) -> Unit) =
            callbackUrl.observe(owner, Observer<String> { data -> callback.invoke(data) })

    fun observeCallbackUrl(owner: Fragment, callback: (data: String?) -> Unit) =
            callbackUrl.observe(owner, Observer<String> { data -> callback.invoke(data) })

    fun observeHasToken(owner: FragmentActivity, callback: (data: Boolean?) -> Unit) =
            hasToken.observe(owner, Observer<Boolean> { data -> callback.invoke(data) })

    fun observeHasToken(owner: Fragment, callback: (data: Boolean?) -> Unit) =
            hasToken.observe(owner, Observer<Boolean> { data -> callback.invoke(data) })

    fun observeLinkedAccounts(owner: FragmentActivity, callback: (data: List<AccountEntity>?) -> Unit) =
            accounts.observe(owner, Observer<List<AccountEntity>> { data -> callback.invoke(data) })

    fun observeLinkedAccounts(owner: Fragment, callback: (data: List<AccountEntity>?) -> Unit) =
            accounts.observe(owner, Observer<List<AccountEntity>> { data -> callback.invoke(data) })

    fun getCallbackUrl(consumerKey: String, consumerSecret: String) {
        TwitterRepository.Consumer.key = consumerKey
        TwitterRepository.Consumer.secret = consumerSecret

        GlobalScope.launch(Dispatchers.IO) {
            TwitterRepository.Token.getRequestToken { callbackUrl.postValue(it) }
        }
    }

    fun getAccessToken(pin: String) {
        GlobalScope.launch(Dispatchers.IO) {
            TwitterRepository.Token.getAccessToken(pin) { hasToken.postValue(it) }
        }
    }

    fun loadAccessToken() {
        GlobalScope.launch(Dispatchers.IO) {
            TwitterRepository.Token.loadAccessToken()
        }
    }

    fun refreshLinkedAccounts() {
        GlobalScope.launch(Dispatchers.IO) {
            accounts.postValue(TwitterRepository.Account.getAll())
        }
    }
}

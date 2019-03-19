package net.totoraj.tjdeck

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProviders
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class AccountLinkageSettingsViewModel : ViewModel() {
    companion object {
        fun getModel(owner: FragmentActivity) = ViewModelProviders.of(owner).get(AccountLinkageSettingsViewModel::class.java)
    }

    private val callbackUrl = MutableLiveData<Uri>()
    private val isPostOnlyAccountLinked = MutableLiveData<Boolean>()

    fun getCallbackUrl() {
        GlobalScope.launch(Dispatchers.IO) {
            // todo call requestToken api
            // todo if success convert callback_url to uri
            val uri = Uri.parse("https://google.com")
            callbackUrl.postValue(uri)
        }
    }

    fun observeCallbackUrl(owner: FragmentActivity, callee: (data: Uri?) -> Unit) {
        callbackUrl.observe(owner, Observer<Uri> { data -> callee.invoke(data) })
    }

    fun getAccessToken() {
        GlobalScope.launch(Dispatchers.IO) {
            // todo call get authToken api
            // todo if success save token in pref
            isPostOnlyAccountLinked.postValue(true)
        }
    }

    fun observeIsLinked(owner: FragmentActivity, callee: (data: Boolean?) -> Unit) {
        isPostOnlyAccountLinked.observe(owner, Observer<Boolean> { data -> callee.invoke(data) })
    }

    fun checkIsLinked() {
        // todo get authToken from pref
        // authToken?.let{
        // todo verifyCredentials(Twitter4j API) and if postValue(result verifyCredentials)
        isPostOnlyAccountLinked.postValue(true)
        // } ?: isPostOnlyAccountLinked.postValue(false)
    }
}

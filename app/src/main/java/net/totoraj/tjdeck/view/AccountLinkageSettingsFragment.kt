package net.totoraj.tjdeck.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import kotlinx.android.synthetic.main.fragment_account_linkage_settings.*
import net.totoraj.tjdeck.R
import net.totoraj.tjdeck.exception.AccessTokenException
import net.totoraj.tjdeck.exception.RequestTokenException
import net.totoraj.tjdeck.model.repository.TwitterRepository
import net.totoraj.tjdeck.viewmodel.TwitterViewModel


class AccountLinkageSettingsFragment : Fragment(), OnBackPressedCallback {
    companion object {
        fun newInstance() = AccountLinkageSettingsFragment()
    }

    private lateinit var mActivity: FragmentActivity
    private lateinit var viewModel: TwitterViewModel
    private var isCancelable = true

    private lateinit var closeButton: ImageView
    private lateinit var cKeyEditor: EditText
    private lateinit var cSecretEditor: EditText
    private lateinit var requestButton: TextView
    private lateinit var pinEditor: EditText
    private lateinit var linkButton: TextView
    private lateinit var loadingIndicator: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            mActivity = requireActivity().apply {
                addOnBackPressedCallback(this@AccountLinkageSettingsFragment, this@AccountLinkageSettingsFragment)
            }

            viewModel = TwitterViewModel.getModel(mActivity).apply {
                observeThrowable(this@AccountLinkageSettingsFragment) {
                    it ?: return@observeThrowable

                    isCancelable = true
                    dismissLoading()
                    when (it.second) {
                        is RequestTokenException -> resetViews()
                        is AccessTokenException -> resetGetAuthTokenViews()
                    }
                }

                observeCallbackUrl(this@AccountLinkageSettingsFragment) { urlString ->
                    urlString ?: return@observeCallbackUrl

                    isCancelable = true
                    dismissLoading()
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urlString)))
                    pinEditor.isEnabled = true
                }

                observeAccessToken(this@AccountLinkageSettingsFragment) { accessToken ->
                    accessToken ?: return@observeAccessToken

                    isCancelable = true
                    dismissLoading()
                    Toast.makeText(mActivity, "linked: ${accessToken.screenName}", Toast.LENGTH_LONG).show()
                    resetGetAuthTokenViews()
                    refreshLinkedAccounts()
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_account_linkage_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        closeButton = button_close
        cKeyEditor = editor_consumer_key
        cSecretEditor = editor_consumer_secret
        requestButton = button_token_request
        pinEditor = editor_pin
        linkButton = button_account_link
        loadingIndicator = indicator_loading
        initViews()
    }

    override fun handleOnBackPressed(): Boolean {
        if (!this.isVisible) return false
        if (!isCancelable) return true

        resetViews()
        viewModel.loadAccessToken()
        requireFragmentManager().beginTransaction().run {
            hide(this@AccountLinkageSettingsFragment)
            commit()
        }
        return true
    }

    private fun initViews() {
        view!!.setOnTouchListener { _, _ -> true }
        loadingIndicator.setOnTouchListener { _, _ -> true }
        closeButton.setOnClickListener { handleOnBackPressed() }

        cKeyEditor.run {
            setText(TwitterRepository.Consumer.key, TextView.BufferType.NORMAL)
            isEnabled = text.isEmpty()
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    // do nothing
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    // do nothing
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    s?.let {
                        requestButton.isEnabled =
                                s.isNotEmpty() && cSecretEditor.text.isNotEmpty()
                    }
                }
            })
        }

        cSecretEditor.run {
            setText(TwitterRepository.Consumer.secret, TextView.BufferType.NORMAL)
            isEnabled = text.isEmpty()
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    // do nothing
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    // do nothing
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    s?.let {
                        requestButton.isEnabled =
                                s.isNotEmpty() && cKeyEditor.text.isNotEmpty()
                    }
                }
            })
        }

        requestButton.run {
            isEnabled = !(cKeyEditor.isEnabled && cSecretEditor.isEnabled)
            setOnClickListener {
                isCancelable = false
                showLoading()

                viewModel.getRequestToken(
                        cKeyEditor.text.toString(),
                        cSecretEditor.text.toString()
                )
            }
        }

        pinEditor.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // do nothing
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // do nothing
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.let { linkButton.isEnabled = s.isNotEmpty() }
            }
        })

        linkButton.setOnClickListener {
            isCancelable = false
            showLoading()
            viewModel.getAccessToken(pinEditor.text.toString())
        }
    }


    private fun resetViews() {
        resetGetRequestTokenViews()
        resetGetAuthTokenViews()
    }

    private fun resetGetRequestTokenViews() {
        cKeyEditor.run {
            setText(TwitterRepository.Consumer.key, TextView.BufferType.NORMAL)
            isEnabled = text.isEmpty()
        }

        cSecretEditor.run {
            setText(TwitterRepository.Consumer.secret, TextView.BufferType.NORMAL)
            isEnabled = text.isEmpty()
        }

        requestButton.isEnabled =
                !(cKeyEditor.isEnabled && cSecretEditor.isEnabled)
    }

    private fun resetGetAuthTokenViews() {
        pinEditor.run {
            text.clear()
            isEnabled = false
        }

        linkButton.isEnabled = false
    }

    private fun showLoading() {
        loadingIndicator.visibility = View.VISIBLE
    }

    private fun dismissLoading() {
        loadingIndicator.visibility = View.GONE
    }
}

package net.totoraj.tjdeck

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity


class AccountLinkageSettingsFragment : Fragment(), OnBackPressedCallback {
    companion object {
        fun newInstance() = AccountLinkageSettingsFragment()
    }

    private lateinit var mActivity: FragmentActivity
    private lateinit var viewModel: AccountLinkageSettingsViewModel
    private var isCancelable = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            mActivity = requireActivity().apply {
                addOnBackPressedCallback(this@AccountLinkageSettingsFragment, this@AccountLinkageSettingsFragment)
            }
            viewModel = AccountLinkageSettingsViewModel.getModel(mActivity).apply {
                observeCallbackUrl(this@AccountLinkageSettingsFragment) { urlString ->
                    urlString?.let {
                        isCancelable = true
                        if (urlString.isEmpty()) {
                            Toast.makeText(mActivity, "incorrect Consumer Key/Secret", Toast.LENGTH_LONG).show()
                            resetViews()
                        } else {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urlString)))
                        }
                    }
                }

                observeHasToken(this@AccountLinkageSettingsFragment) { hasToken ->
                    hasToken?.let {
                        isCancelable = true
                        if (hasToken) {
                            viewModel.refreshLinkedAccounts()
                        } else {
                            Toast.makeText(mActivity, "incorrect PIN", Toast.LENGTH_LONG).show()
                            resetGetAuthTokenViews()
                        }
                    }
                }

                observeLinkedAccounts(this@AccountLinkageSettingsFragment) { accounts ->
                    accounts?.let {
                        if (accounts.isEmpty()) return@observeLinkedAccounts
                        handleOnBackPressed()
                    }
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.account_linkage_settings_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
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

    private fun initViews(view: View) {
        view.run {
            // 透過抑止
            setOnTouchListener { _, _ -> true }

            findViewById<ImageView>(R.id.button_close).run {
                setOnClickListener { handleOnBackPressed() }
            }

            initGetRequestTokenViews(this)
            initGetAuthTokenViews(this)
        }
    }

    private fun initGetRequestTokenViews(view: View) {
        view.run {
            val inputKey = findViewById<EditText>(R.id.editor_consumer_key).apply {
                setText(TwitterRepository.Consumer.key, TextView.BufferType.NORMAL)
                isEnabled = text.isEmpty()
            }
            val inputSecret = findViewById<EditText>(R.id.editor_consumer_secret).apply {
                setText(TwitterRepository.Consumer.secret, TextView.BufferType.NORMAL)
                isEnabled = text.isEmpty()
            }
            val requestButton = findViewById<TextView>(R.id.button_token_request).apply {
                isEnabled = !(inputKey.isEnabled && inputSecret.isEnabled)
                setOnClickListener {
                    isCancelable = false
                    viewModel.getCallbackUrl(inputKey.editableText.toString(), inputSecret.editableText.toString())
                }
            }

            inputKey.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    // do nothing
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    // do nothing
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    s?.let { requestButton.isEnabled = s.isNotEmpty() && inputSecret.editableText.isNotEmpty() }
                }
            })
            inputSecret.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    // do nothing
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    // do nothing
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    s?.let { requestButton.isEnabled = s.isNotEmpty() && inputKey.editableText.isNotEmpty() }
                }
            })
        }
    }

    private fun initGetAuthTokenViews(view: View) {
        view.run {
            val inputPin = findViewById<EditText>(R.id.editor_pin)
            val linkButton = findViewById<TextView>(R.id.button_account_link)

            viewModel.observeCallbackUrl(this@AccountLinkageSettingsFragment) {
                it?.let { inputPin.isEnabled = true }
            }

            linkButton.setOnClickListener {
                isCancelable = false
                viewModel.getAccessToken(inputPin.editableText.toString())
            }

            inputPin.addTextChangedListener(object : TextWatcher {
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
        }
    }

    private fun resetViews() {
        resetGetRequestTokenViews()
        resetGetAuthTokenViews()
    }

    private fun resetGetRequestTokenViews() {
        view!!.run {
            val inputKey = findViewById<EditText>(R.id.editor_consumer_key).apply {
                setText(TwitterRepository.Consumer.key, TextView.BufferType.NORMAL)
                isEnabled = text.isEmpty()
            }

            val inputSecret = findViewById<EditText>(R.id.editor_consumer_secret).apply {
                setText(TwitterRepository.Consumer.secret, TextView.BufferType.NORMAL)
                isEnabled = text.isEmpty()
            }

            findViewById<TextView>(R.id.button_token_request).isEnabled =
                    !(inputKey.isEnabled && inputSecret.isEnabled)
        }
    }

    private fun resetGetAuthTokenViews() {
        view!!.run {
            findViewById<EditText>(R.id.editor_pin).run {
                editableText.clear()
                isEnabled = false
            }

            findViewById<TextView>(R.id.button_account_link).isEnabled = false
        }
    }

}

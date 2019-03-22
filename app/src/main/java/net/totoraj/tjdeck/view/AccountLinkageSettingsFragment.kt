package net.totoraj.tjdeck.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import net.totoraj.tjdeck.R
import net.totoraj.tjdeck.model.exception.AccessTokenException
import net.totoraj.tjdeck.model.exception.RequestTokenException
import net.totoraj.tjdeck.model.repository.TwitterRepository
import net.totoraj.tjdeck.viewmodel.TwitterViewModel


class AccountLinkageSettingsFragment : Fragment(), OnBackPressedCallback {
    companion object {
        fun newInstance() = AccountLinkageSettingsFragment()
    }

    private lateinit var mActivity: FragmentActivity
    private lateinit var viewModel: TwitterViewModel
    private var isCancelable = true

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
                    when (it.second) {
                        is RequestTokenException -> {
                            resetViews()
                        }
                        is AccessTokenException -> {
                            isCancelable = true
                            resetGetAuthTokenViews()
                        }
                    }
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

            val inputKey = findViewById<EditText>(R.id.editor_consumer_key).apply {
                setText(TwitterRepository.Consumer.key, TextView.BufferType.NORMAL)
                isEnabled = text.isEmpty()
            }

            val inputSecret = findViewById<EditText>(R.id.editor_consumer_secret).apply {
                setText(TwitterRepository.Consumer.secret, TextView.BufferType.NORMAL)
                isEnabled = text.isEmpty()
            }

            val inputPin = findViewById<EditText>(R.id.editor_pin)

            val requestButton = findViewById<TextView>(R.id.button_token_request).apply {
                isEnabled = !(inputKey.isEnabled && inputSecret.isEnabled)
                setOnClickListener {
                    isCancelable = false
                    viewModel.getRequestToken(inputKey.editableText.toString(), inputSecret.editableText.toString())
                }
            }

            viewModel.observeCallbackUrl(this@AccountLinkageSettingsFragment) { urlString ->
                urlString ?: return@observeCallbackUrl

                isCancelable = true
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urlString)))
                inputPin.isEnabled = true
            }

            val linkButton = findViewById<TextView>(R.id.button_account_link).apply {
                setOnClickListener {
                    isCancelable = false
                    viewModel.getAccessToken(inputPin.editableText.toString())
                }
            }

            viewModel.observeAccessToken(this@AccountLinkageSettingsFragment) { accessToken ->
                accessToken ?: return@observeAccessToken

                isCancelable = true
                Toast.makeText(mActivity, "linked: ${accessToken.screenName}", Toast.LENGTH_LONG).show()
                resetGetAuthTokenViews()
                viewModel.refreshLinkedAccounts()
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

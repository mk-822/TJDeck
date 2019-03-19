package net.totoraj.tjdeck

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView


class AccountLinkageSettingsFragment : Fragment() {

    companion object {
        fun newInstance() = AccountLinkageSettingsFragment()
    }

    private lateinit var mActivity: FragmentActivity
    private lateinit var viewModel: AccountLinkageSettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            mActivity = requireActivity()
            viewModel = AccountLinkageSettingsViewModel.getModel(mActivity).apply {
                observeCallbackUrl(mActivity) { uri ->
                    uri?.let {
                        startActivity(Intent(Intent.ACTION_VIEW, uri))
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

    private fun initViews(view: View) {
        view.run {
            setOnTouchListener { _, _ -> true }

            findViewById<ImageView>(R.id.button_close).run {
                setOnClickListener { requireActivity().onBackPressed() }
            }

            initGetRequestTokenViews(this)
            initGetAuthTokenViews(this)

        }
    }

    private fun initGetRequestTokenViews(view: View) {
        view.run {
            val inputKey = findViewById<EditText>(R.id.editor_consumer_key)
            val inputSecret = findViewById<EditText>(R.id.editor_consumer_secret)
            val requestButton = findViewById<TextView>(R.id.button_token_request).apply {
                setOnClickListener { if (isEnabled) viewModel.getCallbackUrl() }
            }

            inputKey.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    // do nothing
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    // do nothing
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    s?.let { requestButton.isEnabled = s.isNotEmpty() && inputSecret.text.isNotEmpty() }
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
                    s?.let { requestButton.isEnabled = s.isNotEmpty() && inputKey.text.isNotEmpty() }
                }
            })
        }
    }

    private fun initGetAuthTokenViews(view: View) {
        view.run {
            val linkButton = findViewById<TextView>(R.id.button_account_link).apply {
                setOnClickListener { if (isEnabled) viewModel.getAccessToken() }
            }

            findViewById<EditText>(R.id.editor_pin).run {
                addTextChangedListener(object : TextWatcher {
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
    }

}

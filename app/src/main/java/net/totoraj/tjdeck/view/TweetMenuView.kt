package net.totoraj.tjdeck.view

import android.content.Context
import android.net.Uri
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.layout_tweet_menu.view.*
import net.totoraj.tjdeck.R
import net.totoraj.tjdeck.adapter.LinkedAccountAdapter
import net.totoraj.tjdeck.adapter.UploadItemAdapter
import net.totoraj.tjdeck.model.database.entity.AccountEntity
import kotlin.math.floor

class TweetMenuView : ConstraintLayout {
    companion object {
        const val REQUEST_CODE_CHOOSE_FILE = 1001
    }

    private val maxTweetLength = resources.getInteger(R.integer.max_tweet_length)
    private val maxMediaWithTweet = resources.getInteger(R.integer.max_media_with_tweet)

    val linkedAccounts: RecyclerView
    val tweetContainer: View
    val tweetEdit: EditText
    val inputCharsIndicator: ProgressBar
    val uploadItems: RecyclerView
    val tweetButton: TextView
    val addMediaButton: ImageView

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttrs: Int) : super(context, attrs, defStyleAttrs) {
        inflate(context, R.layout.layout_tweet_menu, this)
        linkedAccounts = accounts_linked
        tweetContainer = container_tweet
        tweetEdit = editor_tweet
        inputCharsIndicator = indicator_input_chars
        uploadItems = items_upload
        tweetButton = button_tweet
        addMediaButton = button_add_media
        init()
    }

    private fun init() {
        tweetContainer.isActivated = false

        addMediaButton.run {
            isActivated = false
            isEnabled = false
        }

        tweetEdit.run {
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    // do nothing
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    // do nothing
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    s?.let {
                        tweetButton.isEnabled = s.isNotEmpty() || uploadItems.adapter?.itemCount != 0
                        inputCharsIndicator.progress =
                                floor((s.length.toFloat() / maxTweetLength) * 100).toInt()
                    }
                }
            })
        }

        linkedAccounts.run {
            layoutManager = GridLayoutManager(context, 6)
            addItemDecoration(TransparentOffsetDecoration(R.dimen.linked_account_offset))
        }

        uploadItems.run {
            layoutManager = GridLayoutManager(context, 2)
            addItemDecoration(TransparentOffsetDecoration(R.dimen.upload_item_offset))
        }
    }

    fun adaptAccount(accounts: List<AccountEntity>) {
        val exists = accounts.isNotEmpty()
        tweetContainer.isActivated = exists
        linkedAccounts.run {
            visibility = if (exists) View.VISIBLE else View.GONE
            (adapter as LinkedAccountAdapter).updateAccounts(accounts)
        }
        tweetEdit.isEnabled = exists
        addMediaButton.run {
            isActivated = exists
            isEnabled = exists
        }
    }

    fun adaptPreview(items: List<Uri>) {
        val exists = items.isNotEmpty()
        uploadItems.run {
            visibility = if (exists) View.VISIBLE else View.GONE
            (adapter as UploadItemAdapter).updateItems(items)
        }
        tweetButton.isEnabled = exists || tweetEdit.text.isNotEmpty()
        addMediaButton.run {
            if (items.size >= maxMediaWithTweet) {
                isActivated = false
                isEnabled = false
            } else {
                isActivated = true
                isEnabled = true
            }
        }
    }
}
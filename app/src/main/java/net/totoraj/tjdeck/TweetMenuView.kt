package net.totoraj.tjdeck

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import twitter4j.TwitterException
import kotlin.math.floor

class TweetMenuView : ConstraintLayout {

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttrs: Int) : super(context, attrs, defStyleAttrs) {
        init()
    }

    private fun init() {
        inflate(context, R.layout.layout_tweet_input, this)
        val tweetEdit = findViewById<EditText>(R.id.editor_tweet)
        val inputCharsIndicator = findViewById<ProgressBar>(R.id.indicator_input_chars)
        val tweetButton = findViewById<TextView>(R.id.button_tweet)

        tweetButton.run {
            setOnClickListener {
                val tweet = tweetEdit.editableText.toString()
                tweetEdit.editableText.clear()
                GlobalScope.launch(Dispatchers.IO) {
                    TwitterRepository.tweet(tweet) { exception ->
                        exception?.let { tweetCallback(exception) }
                    }
                }
            }
        }

        val maxLength = resources.getInteger(R.integer.max_tweet_length)
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
                        tweetButton.isEnabled = s.isNotEmpty()
                        inputCharsIndicator.progress = floor((s.length.toFloat() / maxLength) * 100).toInt()
                    }
                }

            })
        }
    }

    private fun tweetCallback(exception: Exception) {
        val message = when (exception) {
            is TwitterException -> exception.errorMessage
            else -> "unexpected error occurred"
        }

        GlobalScope.launch(Dispatchers.Main) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
}
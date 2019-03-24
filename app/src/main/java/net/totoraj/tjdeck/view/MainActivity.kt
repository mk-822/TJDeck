package net.totoraj.tjdeck.view

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.format.DateFormat
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.webkit.*
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.drawerlayout.widget.DrawerLayout.DrawerListener
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.activity_main.*
import net.totoraj.tjdeck.BuildConfig
import net.totoraj.tjdeck.R
import net.totoraj.tjdeck.adapter.UploadItemAdapter
import net.totoraj.tjdeck.callback.SwipeToDeleteCallback
import net.totoraj.tjdeck.viewmodel.TwitterViewModel
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.*
import java.util.regex.Pattern

class MainActivity : AppCompatActivity(), OnBackPressedCallback {
    companion object {
        const val TAG = "TJDeck"
        const val INPUT_FILE_REQUEST_CODE = 1
        const val TWEET_DECK = "https://tweetdeck.twitter.com"
    }

    private lateinit var mWebView: WebView
    private lateinit var videoFrame: FrameLayout
    private var mFilePathCallback: ValueCallback<Array<Uri>>? = null
    private var mCameraPhotoPath: String? = null

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var accountLinkageSettings: AccountLinkageSettingsFragment
    private lateinit var navigationView: NavigationView
    private lateinit var tweetMenuView: TweetMenuView
    private lateinit var viewModel: TwitterViewModel

    /* WebViewの内容を保持する */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mWebView.saveState(outState)
    }

    /* WebViewの保持した内容を戻す */
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        mWebView.restoreState(savedInstanceState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        addOnBackPressedCallback(this, this)

        drawerLayout = drawer_layout
        accountLinkageSettings = AccountLinkageSettingsFragment.newInstance()
        navigationView = navigation_view
        tweetMenuView = tweet_menu
        mWebView = web_view
        videoFrame = video_view_frame

        viewModel = TwitterViewModel.getModel(this).apply {
            observeThrowable(this@MainActivity) {
                it ?: return@observeThrowable

                Toast.makeText(this@MainActivity, it.first, Toast.LENGTH_LONG).show()
                Log.e("MainActivity", "error occurred", it.second)
            }
        }

        supportFragmentManager.beginTransaction().run {
            replace(R.id.container_account_linkage_settings, accountLinkageSettings)
            hide(accountLinkageSettings)
            commit()
        }

        initDrawer()
        initTweetMenu()
        viewModel.refreshLinkedAccounts()

        initWebView()
        if (savedInstanceState == null) mWebView.loadUrl(TWEET_DECK)

    }

    override fun onResume() {
        super.onResume()

        if (videoFrame.visibility == View.VISIBLE) hideSystemUi()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            INPUT_FILE_REQUEST_CODE -> callbackChromeClientOnShowFileChooser(resultCode, data)
            TweetMenuView.REQUEST_CODE_CHOOSE_FILE -> callbackChoseFile(resultCode, data)
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun handleOnBackPressed(): Boolean {
        return when {
            drawerLayout.isDrawerOpen(GravityCompat.END) -> {
                drawerLayout.closeDrawer(GravityCompat.END)
                true
            }
            videoFrame.visibility != View.VISIBLE && mWebView.canGoBack() -> {
                mWebView.goBack()
                true
            }
            else -> false
        }
    }

    private fun initDrawer() {
        drawerLayout.run {
            addDrawerListener(object : DrawerListener {
                val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

                override fun onDrawerStateChanged(newState: Int) {
                    // do nothing
                }

                override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                    // do nothing
                }

                override fun onDrawerClosed(drawerView: View) {
                    inputMethodManager.hideSoftInputFromWindow(
                            windowToken,
                            InputMethodManager.HIDE_NOT_ALWAYS
                    )
                    requestFocus()
                }

                override fun onDrawerOpened(drawerView: View) {
                    // do nothing
                }
            })
        }

        navigationView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.menu_show_tjdeck_option -> {
                    mWebView.evaluateJavascript("tj_deck.showOptionPanel()", null)
                }
                R.id.menu_post_only_linked_option -> {
                    supportFragmentManager.beginTransaction().run {
                        show(accountLinkageSettings)
                        commit()
                    }
                }
                R.id.menu_oss_license -> {
                    startActivity(Intent(this, OssLicenseActivity::class.java))
                }
            }
            drawerLayout.closeDrawer(GravityCompat.END)
            true
        }
    }

    private fun initTweetMenu() {
        tweetMenuView.run {
            tweetButton.run {
                setOnClickListener {
                    val tweet = tweetEdit.text.toString()
                    tweetEdit.text.clear()
                    viewModel.tweet(tweet)
                }
            }

            addMediaButton.run {
                setOnClickListener {
                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        // 公式アプリでも画像しか選べない
                        // type = "*/*"
                        // putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
                        type = "image/*"
                        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    }
                    startActivityForResult(intent, TweetMenuView.REQUEST_CODE_CHOOSE_FILE)
                }
            }

            uploadItems.run {
                viewModel.observeFiles(this@MainActivity) {
                    it ?: return@observeFiles

                    uploadItems.run {
                        if (adapter == null) adapter = UploadItemAdapter(listOf())
                    }
                    adaptPreview(it)
                }


                val itemTouchHelper = ItemTouchHelper(object : SwipeToDeleteCallback(ItemTouchHelper.START) {
                    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                        viewModel.removeFile(viewHolder.adapterPosition)
                    }
                })
                itemTouchHelper.attachToRecyclerView(this)
            }

            viewModel.run {
                observeLinkedAccounts(this@MainActivity) { accounts ->
                    accounts ?: return@observeLinkedAccounts

                    if (accounts.isNotEmpty()) {
                        tweetEdit.isEnabled = true
                        addMediaButton.run {
                            isActivated = true
                            isEnabled = true
                        }
                        // todo adapt account icon list
                    }
                }
            }
        }

    }

    private fun callbackChoseFile(resultCode: Int, intent: Intent?) {
        if (resultCode != Activity.RESULT_OK || intent == null) return

        val files = mutableListOf<Uri>()
        intent.clipData?.let {
            for (i in 0 until it.itemCount) {
                files.add(it.getItemAt(i).uri)
            }
        } ?: intent.data?.let {
            files.add(it)
        } ?: return

        viewModel.addFiles(files)
    }

    private fun callbackChromeClientOnShowFileChooser(resultCode: Int, data: Intent?) {
        mFilePathCallback ?: return

        var results: Array<Uri>? = null

        // Check that the response is a good one
        if (resultCode == Activity.RESULT_OK) {
            if (data == null) {
                // If there is not data, then we may have taken a photo
                if (mCameraPhotoPath != null) {
                    results = arrayOf(Uri.parse(mCameraPhotoPath))
                }
            } else {
                val dataString = data.dataString
                if (dataString != null) {
                    results = arrayOf(Uri.parse(dataString))
                }
            }
        }

        mFilePathCallback!!.onReceiveValue(results)
        mFilePathCallback = null
        return
    }

    fun hideSystemUi() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    fun showSystemUi() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        mWebView.run {
            webViewClient = TJClient()
            webChromeClient = TJChromeClient(this, videoFrame)
            settings.run {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
            }
        }
    }

    /* アセットからjsファイルを読み込んでStringで返すやつ */
    @Throws(Exception::class)
    private fun loadAssets(fileName: String): String {
        val res = StringBuilder()
        val br = BufferedReader(InputStreamReader(assets.open(fileName), "UTF-8"))

        for (line in br.readLines()) {
            res.append(line)
        }

        return res.substring(0)
    }

    // https://github.com/googlearchive/chromium-webview-samples/blob/master/input-file-example/app/src/main/java/inputfilesample/android/chrome/google/com/inputfilesample/MainFragment.java
    inner class TJChromeClient(
            private val webView: WebView,
            private val videoFrame: FrameLayout
    ) : WebChromeClient() {

        private var customViewCallback: CustomViewCallback? = null
        private var videoView: View? = null

        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
            super.onShowCustomView(view, callback)

            customViewCallback = callback

            hideSystemUi()

            if (videoView != null) videoFrame.removeView(videoView)
            videoView = view
            videoFrame.addView(
                    videoView,
                    ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            )

            webView.visibility = View.INVISIBLE
            videoFrame.visibility = View.VISIBLE
        }

        override fun onHideCustomView() {
            super.onHideCustomView()

            customViewCallback?.onCustomViewHidden()
            customViewCallback = null

            showSystemUi()

            videoFrame.removeView(videoView)
            videoView = null

            webView.visibility = View.VISIBLE
            videoFrame.visibility = View.INVISIBLE
        }

        override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: WebChromeClient.FileChooserParams
        ): Boolean {
            if (mFilePathCallback != null) {
                mFilePathCallback!!.onReceiveValue(null)
            }
            mFilePathCallback = filePathCallback

            var takePictureIntent: Intent? = Intent(MediaStore.INTENT_ACTION_MEDIA_SEARCH)
            if (takePictureIntent!!.resolveActivity(packageManager) != null) {
                var photoFile: File? = null
                try {
                    photoFile = createImageFile()
                    takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath)
                } catch (error: IOException) {

                }

                if (photoFile != null) {
                    mCameraPhotoPath = "file:" + photoFile.absolutePath
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile))
                } else {
                    takePictureIntent = null
                }
            }

            val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
            contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
            contentSelectionIntent.type = "*/*"

            val intentArray: Array<Intent> = if (takePictureIntent != null) {
                arrayOf(takePictureIntent)
            } else {
                arrayOf()
            }

            val chooserIntent = Intent(Intent.ACTION_CHOOSER)
            chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
            chooserIntent.putExtra(Intent.EXTRA_TITLE, "Media Chooser")
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
            chooserIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            val mimeTypes = arrayOf("image/*", "video/*")
            chooserIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)

            startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE)

            return true
        }

        @Throws(IOException::class)
        private fun createImageFile(): File {
            val timeStamp = DateFormat.format("yyyyMMdd_HHmmss", Date()).toString()
            val storageDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOCUMENTS)
            return File.createTempFile(timeStamp, "*", storageDir)
        }
    }

    /* WebViewのクライアント */
    inner class TJClient : WebViewClient() {
        private val regexDeck = "^https://tweetdeck\\.twitter\\.com.*$"
        private val regexTwitter = "^https://(.+\\.|)twitter\\.com/(login|logout|sessions|account/login_verification).*$"
        private var tjDeckScript = ""
        private var tjCheckScript = ""

        init {
            /* jsの用意 */
            try {
                tjDeckScript = if (BuildConfig.DEBUG) {
                    Log.d(TAG, "デバッグ用")
                    loadAssets("tj-deck-debug.js")
                } else {
                    Log.d(TAG, "リリース用")
                    loadAssets("tj-deck.js")
                }
                tjCheckScript = loadAssets("test.js")
            } catch (error: Exception) {
                Log.d(TAG, "onError")
            }
        }

        // ページのロード終了時
        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            runTJDeckScript(view, url)
        }

        // URLが開かれたときにチェックする
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val url = request.url.toString()
            Log.d(TAG, url)
            val itDeck = Pattern.matches(regexDeck, url)
            val itTwiLogin = Pattern.matches(regexTwitter, url)

            // DeckでもTwitterログインページでもなければブラウザに飛ばす
            if (!itDeck && !itTwiLogin) {
                view.stopLoading()
                val intent = Intent(Intent.ACTION_VIEW, request.url)
                startActivity(intent)
                return false
            }

            return super.shouldOverrideUrlLoading(view, request)
        }

        /* tj-deck.jsを実行する */
        private fun runTJDeckScript(view: WebView, url: String) {

            val itDeck = Pattern.matches(regexDeck, url)
            val itTwiLogin = Pattern.matches(regexTwitter, url)
            Log.d(TAG, url)
            when {
                itDeck -> {
                    Log.d(TAG, "TweetDeckです")

                    // TweetDeckにログインしていてなおかつtj-deckが実行されていないか確認する
                    view.evaluateJavascript(tjCheckScript) { value ->
                        Log.d(TAG, value)
                        if (java.lang.Boolean.parseBoolean(value)) {
                            // 実行！！
                            view.evaluateJavascript(tjDeckScript, null)
                        } else if (Pattern.matches("^.*/\\?via_twitter_login=true$", url)) {
                            Log.d(TAG, "via_twitter_login=true")
                            view.evaluateJavascript(tjDeckScript, null)
                        }
                    }
                }
                itTwiLogin -> Log.d(TAG, "Twitterのログインページです")
                else -> Log.d(TAG, "それ以外です")
            }
        }
    }
}

package net.totoraj.tjdeck

import android.app.Activity
import android.content.Intent
import android.content.MutableContextWrapper
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.regex.Pattern

class MainActivity : Activity() {
    private val TAG = "TJDeck"
    private val mContent: View? = null
    private var mWebView: WebView? = null
    private var tjDeckScript = ""
    private var tjCheckScript = ""
    private val regexDeck = "^https://tweetdeck\\.twitter\\.com.*$"
    private val regexTwitter = "^https://(.+\\.|)twitter\\.com/(login|logout|sessions|account/login_verification).*$"
    private var mFilePathCallback: ValueCallback<Array<Uri>>? = null
    private var mCameraPhotoPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                hideSystemUI(1000)
            }
        }

        /* jsの用意 */
        try {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "デバッグ用")
                tjDeckScript = loadAssets("tj-deck-debug.js")
            } else {
                Log.d(TAG, "リリース用")
                tjDeckScript = loadAssets("tj-deck.js")
            }
            tjCheckScript = loadAssets("test.js")
        } catch (error: Exception) {
            Log.d(TAG, "onError")
        }

        mWebView = findViewById<View>(R.id.webView1) as WebView

        mWebView!!.webViewClient = TJClient()
        mWebView!!.webChromeClient = TJChromeClient()

        val settings = mWebView!!.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false

        if (savedInstanceState == null) {
            mWebView!!.loadUrl("https://tweetdeck.twitter.com")
        }


    }

    /* アセットからjsファイルを読み込んでStringで返すやつ */
    @Throws(Exception::class)
    private fun loadAssets(fileName: String): String {
        val res = StringBuilder()
        var `is`: InputStream? = null
        var br: BufferedReader? = null
        `is` = assets.open(fileName)
        br = BufferedReader(InputStreamReader(`is`!!, "UTF-8"))

        for(line in br.readLines()){
            res.append(line)
        }

        return res.substring(0)
    }


    /* 戻るボタンでブラウザバックするようにする */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && mWebView!!.canGoBack()) {
            mWebView!!.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    /* WebViewの内容を保持する */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mWebView!!.saveState(outState)
    }

    /* WebViewの内容を保持する */
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        mWebView!!.restoreState(savedInstanceState)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != INPUT_FILE_REQUEST_CODE || mFilePathCallback == null) {
            super.onActivityResult(requestCode, resultCode, data)
            return
        }

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


    // https://github.com/googlearchive/chromium-webview-samples/blob/master/input-file-example/app/src/main/java/inputfilesample/android/chrome/google/com/inputfilesample/MainFragment.java
    inner class TJChromeClient : WebChromeClient() {
        override fun onShowFileChooser(
                webView: WebView, filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: WebChromeClient.FileChooserParams): Boolean {
            if (mFilePathCallback != null) {
                mFilePathCallback!!.onReceiveValue(null)
            }
            mFilePathCallback = filePathCallback

            var takePicutureIntent: Intent? = Intent(MediaStore.INTENT_ACTION_MEDIA_SEARCH)
            if (takePicutureIntent!!.resolveActivity(packageManager) != null) {
                var photoFile: File? = null
                try {
                    photoFile = createImageFile()
                    takePicutureIntent.putExtra("PhotoPath", mCameraPhotoPath)
                } catch (error: IOException) {

                }

                if (photoFile != null) {
                    mCameraPhotoPath = "file:" + photoFile.absolutePath
                    takePicutureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile))
                } else {
                    takePicutureIntent = null
                }
            }

            val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
            contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
            contentSelectionIntent.type = "*/*"

            val intentArray: Array<Intent>
            if (takePicutureIntent != null) {
                intentArray = arrayOf(takePicutureIntent)
            } else {
                intentArray = arrayOf()
            }

            val chooserIntent = Intent(Intent.ACTION_CHOOSER)
            chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
            chooserIntent.putExtra(Intent.EXTRA_TITLE, "Media Chooser")
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
            chooserIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            val mimetypes = arrayOf("image/*", "video/*")
            chooserIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes)

            startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE)

            return true
        }

        @Throws(IOException::class)
        private fun createImageFile(): File {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            val storageDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOCUMENTS)
            return File.createTempFile(timeStamp, "*", storageDir)
        }
    }


    /* WebViewのクライアント */
    inner class TJClient : WebViewClient() {
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


        //        @Override
        //        public void onLoadResource(WebView view, String url) {
        //            Log.d(TAG, "onLoadResource: " + url);
        //        }
        //        @Override
        //        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        //            handler.proceed();
        //        }

        /* tj-deck.jsを実行する */
        private fun runTJDeckScript(view: WebView, url: String) {

            val itDeck = Pattern.matches(regexDeck, url)
            val itTwiLogin = Pattern.matches(regexTwitter, url)
            Log.d(TAG, url)
            if (itDeck) {
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
            } else if (itTwiLogin) {
                Log.d(TAG, "Twitterのログインページです")
            } else {
                Log.d(TAG, "それ以外です")
            }
        }

    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI(0)
        }
    }

    private fun hideSystemUI(delayMillis: Int) {
        val decorView = window.decorView
        Handler(Looper.getMainLooper()).postDelayed({
            decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
        }, delayMillis.toLong())
    }

    companion object {

        val INPUT_FILE_REQUEST_CODE = 1
        val EXTRA_FROM_NOTIFICATION = "EXTRA_FORM_NOTIFICATION"
    }
}

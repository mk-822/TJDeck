package net.totoraj.tjdeck.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_oss.*
import net.totoraj.tjdeck.R

class OssLicenseActivity : AppCompatActivity() {

    private val licenseFile = "file:///android_asset/html/oss.html"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_oss)

        button_close.setOnClickListener { onBackPressed() }
        web_view.run {
            settings.textZoom = 100
            loadUrl(licenseFile)
        }
    }
}
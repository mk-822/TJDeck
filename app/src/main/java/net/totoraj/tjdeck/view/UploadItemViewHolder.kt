package net.totoraj.tjdeck.view

import android.net.Uri
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.layout_upload_item.view.*

class UploadItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val preview = view.img_preview

    fun bind(uri: Uri) = preview.setImageURI(uri)
}
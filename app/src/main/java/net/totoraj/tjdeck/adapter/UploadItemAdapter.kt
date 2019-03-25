package net.totoraj.tjdeck.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.layout_upload_item.view.*
import net.totoraj.tjdeck.MyApplication.Companion.getAppContext
import net.totoraj.tjdeck.R
import net.totoraj.tjdeck.callback.UploadItemDiffCallback
import net.totoraj.tjdeck.callback.UploadItemSwipeCallback.LayeredViewHolder

class UploadItemAdapter(private var items: List<Uri>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    fun updateItems(newItems: List<Uri>) {
        val diffResult = DiffUtil.calculateDiff(UploadItemDiffCallback(items, newItems))
        items = newItems
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            UploadItemViewHolder(
                    LayoutInflater.from(getAppContext())
                            .inflate(R.layout.layout_upload_item, parent, false)
            )

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
            when (holder) {
                is UploadItemViewHolder -> holder.bind(items[position])
                else -> { // do nothing

                }
            }

    inner class UploadItemViewHolder(val view: View) : LayeredViewHolder(view) {
        override val foreground: View = view.fore
        override val background: View = view.back

        fun bind(uri: Uri) = view.img_preview.setImageURI(uri)
    }
}
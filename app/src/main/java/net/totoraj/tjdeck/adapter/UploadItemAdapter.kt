package net.totoraj.tjdeck.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import net.totoraj.tjdeck.MyApplication.Companion.getAppContext
import net.totoraj.tjdeck.R
import net.totoraj.tjdeck.view.UploadItemDiffCallback
import net.totoraj.tjdeck.view.UploadItemViewHolder

class UploadItemAdapter(private var items: List<Uri>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface UploadItemAdapterListener {
        fun deleteItem(item: Uri)
    }

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
}
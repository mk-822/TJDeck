package net.totoraj.tjdeck.view

import android.net.Uri
import androidx.recyclerview.widget.DiffUtil

class UploadItemDiffCallback(
        private val old: List<Uri>,
        private val new: List<Uri>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = old.size

    override fun getNewListSize(): Int = new.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            old[oldItemPosition] == new[newItemPosition]

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            old[oldItemPosition].toString() == new[newItemPosition].toString()
}
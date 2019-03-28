package net.totoraj.tjdeck.callback

import androidx.recyclerview.widget.DiffUtil
import net.totoraj.tjdeck.model.database.entity.AccountEntity

class LinkedAccountsDiffCallback(
        private val old: List<AccountEntity>,
        private val new: List<AccountEntity>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = old.size

    override fun getNewListSize(): Int = new.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            old[oldItemPosition].userId == new[newItemPosition].userId

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            old[oldItemPosition] == new[newItemPosition]

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        return new[newItemPosition].isDefaultUser
    }
}
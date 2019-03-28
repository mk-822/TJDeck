package net.totoraj.tjdeck.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.layout_linked_account.view.*
import net.totoraj.tjdeck.MyApplication.Companion.getAppContext
import net.totoraj.tjdeck.R
import net.totoraj.tjdeck.callback.LinkedAccountsDiffCallback
import net.totoraj.tjdeck.lib.picasso.transformation.CropCircleTransformation
import net.totoraj.tjdeck.model.database.entity.AccountEntity

class LinkedAccountAdapter(private var items: List<AccountEntity>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    interface OnItemClickListener {
        fun onItemClick(account: AccountEntity)
    }

    private var listener: OnItemClickListener? = null

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    fun updateAccounts(newAccounts: List<AccountEntity>) {
        val diffResult = DiffUtil.calculateDiff(LinkedAccountsDiffCallback(items, newAccounts))
        items = newAccounts
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val holder = LinkedAccountViewHolder(
                LayoutInflater.from(getAppContext())
                        .inflate(R.layout.layout_linked_account, parent, false)
        )

        holder.itemView.setOnClickListener {
            listener?.onItemClick(items[holder.adapterPosition])
        }

        return holder
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
            when (holder) {
                is LinkedAccountViewHolder -> holder.bind(items[position])
                else -> { // do nothing
                }
            }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
            return
        }

        if (payloads[0] is Boolean) {
            val isDefaultUser = payloads[0] as Boolean
            holder.itemView.default_mark.visibility = if (isDefaultUser) View.VISIBLE else View.GONE
        }
    }

    inner class LinkedAccountViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        fun bind(account: AccountEntity) {
            Picasso.get()
                    .load(account.iconUrl)
                    .placeholder(R.drawable.ic_dummy_account_circle)
                    .error(R.drawable.ic_dummy_account_circle)
                    .transform(CropCircleTransformation())
                    .into(view.icon)
            view.default_mark.visibility = if (account.isDefaultUser) View.VISIBLE else View.GONE
        }
    }
}
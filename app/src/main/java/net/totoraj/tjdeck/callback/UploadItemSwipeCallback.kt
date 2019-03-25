package net.totoraj.tjdeck.callback

import android.graphics.Canvas
import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

abstract class UploadItemSwipeCallback : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.START) {

    abstract class LayeredViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        abstract val foreground: View
        abstract val background: View
    }

    override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
    ): Boolean {
        // do nothing
        return false
    }

    override fun onChildDraw(
            c: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean
    ) {
        val holder = viewHolder as LayeredViewHolder
        when {
            dX == 0f && !isCurrentlyActive -> {
                // canceled
                getDefaultUIUtil().onDraw(c, recyclerView, holder.background, 0f, 0f, actionState, isCurrentlyActive)
                getDefaultUIUtil().onDraw(c, recyclerView, holder.foreground, 0f, 0f, actionState, isCurrentlyActive)
            }
            dX < 0 -> {
                // backgroundを固定して、foregroundだけを動かす
                getDefaultUIUtil().onDraw(c, recyclerView, holder.background, 0f, 0f, actionState, isCurrentlyActive)
                getDefaultUIUtil().onDraw(c, recyclerView, holder.foreground, dX, dY, actionState, isCurrentlyActive)
            }
        }
    }
}
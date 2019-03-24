package net.totoraj.tjdeck.callback

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.ColorDrawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import net.totoraj.tjdeck.MyApplication
import net.totoraj.tjdeck.R

abstract class SwipeToDeleteCallback(swipeDirs: Int) : ItemTouchHelper.SimpleCallback(0, swipeDirs) {
    private val context = MyApplication.getAppContext()

    override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
    ): Boolean {
        // do nothing
        return false
    }

    private val deleteIcon = ContextCompat.getDrawable(context, R.drawable.ic_delete)
    private val deleteIconIntrinsicWidth = deleteIcon?.intrinsicWidth
    private val deleteIconIntrinsicHeight = deleteIcon?.intrinsicHeight

    private val background = ColorDrawable()
    private val leftBackgroundColor = ContextCompat.getColor(context, R.color.red)
    private val clearPaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }

    override fun onChildDraw(
            c: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView

        val isCanceled = dX == 0f && !isCurrentlyActive
        if (isCanceled) {
            clearCanvas(c, itemView.right + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat())
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            return
        }

        // Draw the delete background
        val isLeftDirection = dX < 0
        if (isLeftDirection) {
            background.color = leftBackgroundColor
            background.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
        }
        background.draw(c)

        val itemHeight = itemView.bottom - itemView.top
        if (deleteIcon != null
                && deleteIconIntrinsicWidth != null
                && deleteIconIntrinsicHeight != null) {

            if (isLeftDirection) {
                // Calculate position of delete icon
                val deleteIconTop = itemView.top + (itemHeight - deleteIconIntrinsicHeight) / 2
                val deleteIconMargin = (itemHeight - deleteIconIntrinsicHeight) / 2
                val deleteIconLeft = itemView.right - deleteIconMargin - deleteIconIntrinsicWidth
                val deleteIconRight = itemView.right - deleteIconMargin
                val deleteIconBottom = deleteIconTop + deleteIconIntrinsicHeight

                // Draw the delete icon
                deleteIcon.setBounds(deleteIconLeft, deleteIconTop, deleteIconRight, deleteIconBottom)
                deleteIcon.draw(c)
            }
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    private fun clearCanvas(c: Canvas?, left: Float, top: Float, right: Float, bottom: Float) {
        c?.drawRect(left, top, right, bottom, clearPaint)
    }
}
package net.totoraj.tjdeck.view

import android.graphics.Rect
import android.view.View
import androidx.annotation.DimenRes
import androidx.recyclerview.widget.RecyclerView
import net.totoraj.tjdeck.MyApplication

class TransparentOffsetDecoration(@DimenRes private val itemOffsetId: Int) : RecyclerView.ItemDecoration() {
    private val itemOffset = MyApplication.getAppContext().resources.getDimensionPixelOffset(itemOffsetId)

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)
        outRect.set(itemOffset, itemOffset, itemOffset, itemOffset)
    }
}
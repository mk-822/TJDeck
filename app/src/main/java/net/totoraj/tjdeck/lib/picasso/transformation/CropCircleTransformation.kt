package net.totoraj.tjdeck.lib.picasso.transformation

import android.graphics.*
import com.squareup.picasso.Transformation

class CropCircleTransformation : Transformation {
    override fun key(): String = "circle()"

    override fun transform(source: Bitmap): Bitmap {
        val (side, cropStartX, cropStartY) =
                source.run {
                    val side = minOf(width, height)
                    return@run Triple(side, (width - side) / 2f, (height - side) / 2f)
                }

        val bitmap = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
        val radius = side / 2f

        val sourceShader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
            if (cropStartX != 0f || cropStartY != 0f) {
                // sourceが正方形でない場合、bitmapの(0,0)にsourceのクロップ開始位置を合わせる
                setLocalMatrix(Matrix().apply { setTranslate(-cropStartX, -cropStartY) })
            }
        }

        val paint = Paint().apply {
            isAntiAlias = true
            shader = sourceShader
        }

        Canvas(bitmap).drawCircle(radius, radius, radius, paint)
        source.recycle()

        return bitmap
    }
}
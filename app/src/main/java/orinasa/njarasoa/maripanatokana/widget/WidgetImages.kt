package orinasa.njarasoa.maripanatokana.widget

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.glance.ImageProvider

/**
 * An image sent to the launcher as pixels rather than as a resource number. After an app update some
 * launchers keep the previous version's resources, and a number that has since moved draws whichever
 * image now sits there (a sun as the background, the share icon for refresh).
 * Pictures are decoded at their file size, not scaled up to the screen density; icons drawn as
 * vectors are rendered at [vectorSizePx].
 */
internal fun widgetImage(context: Context, @DrawableRes res: Int, vectorSizePx: Int = 48): ImageProvider {
    val drawable = ContextCompat.getDrawable(context, res)
    val bitmap = if (drawable is BitmapDrawable) {
        BitmapFactory.decodeResource(context.resources, res, BitmapFactory.Options().apply { inScaled = false })
    } else {
        drawable!!.toBitmap(vectorSizePx, vectorSizePx)
    }
    return ImageProvider(bitmap)
}

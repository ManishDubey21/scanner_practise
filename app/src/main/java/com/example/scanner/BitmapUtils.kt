package com.example.scanner

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream

/**
 * Utility class for compressing, reading, and resizing Bitmaps.
 */
object BitmapUtils {
    /**
     * Decodes a photo into a Bitmap, reducing the size to optimize reading on a RecyclerView.
     *
     * @param mediaPath Path of the media
     * @param reqWidth  Requested width
     * @param reqHeight Requested height
     * @return Decoded Bitmap
     */
    fun decodeSampledBitmapFromResource(mediaPath: String, reqWidth: Int, reqHeight: Int): Bitmap {
        // First decode with inJustDecodeBounds=true to check dimensions
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(mediaPath, options)

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(mediaPath, options)
    }

    /**
     * Calculates the optimal size of the photo.
     *
     * @param options   Decode option
     * @param reqWidth  Requested width
     * @param reqHeight Requested height
     * @return InSampleSize value
     */
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        // Raw height and width of image
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize > reqHeight && halfWidth / inSampleSize > reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * Compresses the Bitmap image.
     *
     * @param bitmap  Bitmap image
     * @param quality Compression quality where 100 is maximum quality
     * @return Byte array of the compressed image
     */
    fun getBytesFromBitmap(bitmap: Bitmap, quality: Int): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        return stream.toByteArray()
    }

    /**
     * Given the path of an image, reads the image bitmap, resizes it in width and height,
     * applies compression on quality, and returns a byte array.
     *
     * @param path    Image path
     * @param quality Integer from 0 to 100 where 100 is an image without any compression
     * @param width   Width
     * @param height  Height
     * @return Byte array of the compressed image
     */
    fun getBytesImageCompressedFromPath(path: String, quality: Int, width: Int, height: Int): ByteArray {
        return getBytesFromBitmap(decodeSampledBitmapFromResource(path, width, height), quality)
    }

}
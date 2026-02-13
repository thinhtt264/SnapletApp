package com.thinh.snaplet.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.RectF
import androidx.core.graphics.createBitmap
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

object FileUtils {

    private const val WEBP_QUALITY = 95

    /**
     * Applies orientation (EXIF + optional horizontal flip) and compresses to WebP. Call on upload.
     *
     * @param file Source image (e.g. JPEG from camera)
     * @param flipHorizontal true = mirror (front camera), false = EXIF normalization only
     * @return Path to the .webp file, or null on failure (original file unchanged)
     */
    fun flipAndCompressImage(file: File, flipHorizontal: Boolean = false): String? {
        if (!file.exists() || !file.canRead()) return null
        val webpFile = File(file.parent, "${file.nameWithoutExtension}.webp")
        return try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return null
            val path = file.absolutePath
            val exif = ExifInterface(path)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            val matrix = matrixForExifOrientation(orientation)
            if (flipHorizontal) matrix.postScale(-1f, 1f)

            val rect = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
            matrix.mapRect(rect)
            matrix.postTranslate(-rect.left, -rect.top)
            val outWidth = rect.width().toInt().coerceAtLeast(1)
            val outHeight = rect.height().toInt().coerceAtLeast(1)

            val result = createBitmap(outWidth, outHeight)
            val canvas = Canvas(result)
            canvas.drawBitmap(bitmap, matrix, null)
            bitmap.recycle()

            FileOutputStream(webpFile).use { out ->
                result.compress(Bitmap.CompressFormat.WEBP, WEBP_QUALITY, out)
            }
            result.recycle()

            ExifInterface(webpFile.absolutePath).apply {
                setAttribute(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL.toString()
                )
                saveAttributes()
            }
            file.delete()
            webpFile.absolutePath
        } catch (e: Exception) {
            Logger.e(e, "flipAndCompressImage failed: ${file.absolutePath}")
            if (webpFile.exists()) webpFile.delete()
            null
        }
    }

    private fun matrixForExifOrientation(orientation: Int): Matrix {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
            else -> { /* ORIENTATION_NORMAL or unknown: identity */
            }
        }
        return matrix
    }

    /** Deletes the file at [filePath]. Returns true if deleted. */
    fun deleteFileFromPath(filePath: String?): Boolean {
        if (filePath == null) {
            return false
        }

        return try {
            val file = File(filePath)
            if (file.exists()) {
                val deleted = file.delete()
                if (!deleted) {
                    Logger.w("⚠️ Failed to delete file (file.delete() returned false): $filePath")
                }
                deleted
            } else {
                false
            }
        } catch (e: Exception) {
            Logger.e(e, "❌ Failed to delete file: $filePath")
            false
        }
    }

    /** Deletes each file at [filePaths]. Returns count of deleted files. */
    fun deleteFilesFromPaths(filePaths: List<String?>): Int {
        return filePaths.count { deleteFileFromPath(it) }
    }
}

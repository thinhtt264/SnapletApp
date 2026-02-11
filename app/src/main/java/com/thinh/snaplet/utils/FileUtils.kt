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

    /**
     * Normalizes EXIF orientation and flips the image horizontally (mirror).
     * Used for front-camera captures so the saved file is upright and matches
     * what the user sees; upload/download are correct.
     *
     * @param file Image file (e.g. JPEG) to transform in place
     * @return true if transform and save succeeded, false otherwise
     */
    fun flipImageFileHorizontally(file: File): Boolean {
        if (!file.exists() || !file.canRead()) return false
        return try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return false
            val path = file.absolutePath

            // 1) Matrix: apply EXIF rotation so image is upright, then flip horizontal
            val exif = ExifInterface(path)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            val matrix = matrixForExifOrientation(orientation)
            matrix.postScale(-1f, 1f)

            // 2) For 90/270 the output size swaps; createBitmap needs correct bounds
            val rect = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
            matrix.mapRect(rect)
            matrix.postTranslate(-rect.left, -rect.top)
            val outWidth = rect.width().toInt().coerceAtLeast(1)
            val outHeight = rect.height().toInt().coerceAtLeast(1)

            val result = createBitmap(outWidth, outHeight)
            val canvas = Canvas(result)
            canvas.drawBitmap(bitmap, matrix, null)
            bitmap.recycle()

            FileOutputStream(file).use { out ->
                result.compress(Bitmap.CompressFormat.JPEG, 99, out)
            }
            result.recycle()

            // 3) Set EXIF to normal so viewers show correct orientation
            ExifInterface(path).apply {
                setAttribute(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL.toString()
                )
                saveAttributes()
            }
            true
        } catch (e: Exception) {
            Logger.e(e, "flipImageFileHorizontally failed: ${file.absolutePath}")
            false
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

    /**
     * Deletes a file from the given file path.
     *
     * @param filePath The absolute path of the file to delete
     * @return true if the file was successfully deleted, false otherwise
     */
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

    /**
     * Deletes multiple files from the given file paths.
     *
     * @param filePaths List of absolute paths of files to delete
     * @return Number of files successfully deleted
     */
    fun deleteFilesFromPaths(filePaths: List<String?>): Int {
        return filePaths.count { deleteFileFromPath(it) }
    }
}

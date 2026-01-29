package com.thinh.snaplet.utils

import java.io.File

object FileUtils {

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

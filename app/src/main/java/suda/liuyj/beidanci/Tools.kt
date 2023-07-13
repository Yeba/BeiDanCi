package suda.liuyj.beidanci

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import java.io.File


fun toast(msg: String, duration: Int, ctx: Context) {
    Toast.makeText(ctx, msg, duration).show()
}

fun checkStorageManagerPermission(msg:String,ctx: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R ||
        Environment.isExternalStorageManager()
    ) {
        Log.v("file", "已获得访问文件权限")
//        Toast.makeText(ctx, "已获得访问文件权限", Toast.LENGTH_SHORT).show()
    } else {
        val builder = AlertDialog.Builder(ctx)
            .setMessage(msg)
            .setPositiveButton("确定") { _, _ ->
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                ctx.startActivity(intent)
            }
        builder.show()
    }
}

fun openBrowser(url: String, ctx: Context) {
    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

fun setClipBoard(s:String,ctx: Context,txtToast:String=""){
    val clipboardManager = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clipData = ClipData.newPlainText(R.string.app_name.toString(), s)
    clipboardManager.setPrimaryClip(clipData)
    if (txtToast!="") Toast.makeText(ctx,txtToast,Toast.LENGTH_LONG).show()
}

object FileUtil {
    /**
     * 专为Android4.4设计的从Uri获取文件绝对路径，以前的方法已不好使
     */
    fun getPath(context: Context, uri: Uri): String? {
        val isKitKat = true

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            //一些三方的文件浏览器会进入到这个方法中，例如ES
            //QQ文件管理器不在此列
            if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                val type = split[0]
                if ("primary".equals(type, ignoreCase = true)) {
                    return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                }
            } else if (isDownloadsDocument(uri)) {
                val id = DocumentsContract.getDocumentId(uri)
                // 不同系统获取的id开头可能不一样，在这后面便是真实的地址
                if (id.startsWith("msf:")) {
                    return id.replaceFirst("msf:".toRegex(), "")
                }
                val contentUri = ContentUris.withAppendedId(
                    Uri.parse("content://downloads/public_downloads"),
                    java.lang.Long.valueOf(id)
                )
                return getDataColumn(context, contentUri, null, null)
            } else if (isMediaDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                val type = split[0]
                var contentUri: Uri? = null
                if ("image" == type) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                } else if ("video" == type) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else if ("audio" == type) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }
                val selection = "_id=?"
                val selectionArgs = arrayOf(split[1])
                return getDataColumn(context, contentUri, selection, selectionArgs)
            }
        } else if ("content".equals(uri.scheme, ignoreCase = true)) { // MediaStore (and general)
            // Return the remote address
            if (isGooglePhotosUri(uri)) return uri.lastPathSegment
            if (isQQMediaDocument(uri)) {
                val path = uri.path
                val fileDir = Environment.getExternalStorageDirectory()
                val file = File(fileDir, path!!.substring("/QQBrowser".length, path.length))
                return if (file.exists()) file.toString() else null
            }
            return getDataColumn(context, uri, null, null)
        } else if ("file".equals(uri.scheme, ignoreCase = true)) { // File
            return uri.path
        }
        return null
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    fun getDataColumn(
        context: Context,
        uri: Uri?,
        selection: String?,
        selectionArgs: Array<String>?
    ): String? {
        var cursor: Cursor? = null
        val column = MediaStore.MediaColumns.DATA
        val projection = arrayOf(column)
        try {
            cursor =
                context.contentResolver.query(uri!!, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                val column_index = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(column_index)
            }
        } finally {
            cursor?.close()
        }
        return null
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    /**
     * 使用第三方qq文件管理器打开
     *
     * @param uri
     * @return
     */
    fun isQQMediaDocument(uri: Uri): Boolean {
        return "com.tencent.mtt.fileprovider" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    fun isGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.content" == uri.authority
    }
}

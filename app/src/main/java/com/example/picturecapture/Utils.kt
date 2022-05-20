package com.example.picturecapture

import android.Manifest
import android.app.Activity
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import com.permissionx.guolindev.PermissionX
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream


object Utils {
    fun Context.requestPermission(
        activity: FragmentActivity,
        onGranted: ((Boolean) -> Unit)? = null,
    ) {
        PermissionX.init(activity)
            .permissions(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            .onExplainRequestReason { scope, deniedList ->
                scope.showRequestReasonDialog(
                    deniedList,
                    getString(R.string.permission_request_explain_msg),
                    getString(R.string.ok),
                    getString(R.string.cancel)
                )
            }
            .onForwardToSettings { scope, deniedList ->
                scope.showForwardToSettingsDialog(
                    deniedList,
                    getString(R.string.permission_denied_msg),
                    getString(R.string.ok),
                    getString(R.string.cancel)
                )
            }
            .request { allGranted, _, _ ->
                if (allGranted) {
                    onGranted?.invoke(true)
                } else {
                    Toast.makeText(this,
                        getString(R.string.permission_setting_msg),
                        Toast.LENGTH_SHORT).show()
                }
            }
    }

    fun Context.checkStoragePermission(): Boolean {
        return (ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED)

    }

    const val IMAGES_FOLDER_NAME = "ProjectImages"

    @Throws(IOException::class)
    fun Activity.saveImage(bitmap: Bitmap, name: String, isImageSaved: ((Uri) -> Unit)? = null) {
        val saved: Boolean
        var imageUri: Uri? = null
        val fos: OutputStream?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver: ContentResolver = contentResolver
            val contentValues = ContentValues()
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/$IMAGES_FOLDER_NAME")
            imageUri =
                resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            fos = imageUri?.let { resolver.openOutputStream(it) }
        } else {
            val imagesDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM).toString() + File.separator + IMAGES_FOLDER_NAME
            val file = File(imagesDir)
            if (!file.exists()) {
                file.mkdir()
            }
            val image = File(imagesDir, "$name.png")
            imageUri = Uri.fromFile(image)
            fos = FileOutputStream(image)
        }
        saved = bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        imageUri?.let { isImageSaved?.invoke(it) }
        fos?.flush()
        fos?.close()
    }

    fun Activity.shareScreenshot(screenshotBitmap: Bitmap, imageText: String) {

        val cachePath = File(externalCacheDir, "share_pictures/")
        cachePath.mkdirs()

        val screenshotFile = File(cachePath, "image.png").also { file ->
            FileOutputStream(file).use { fileOutputStream ->
                screenshotBitmap.compress(Bitmap.CompressFormat.PNG,
                    100,
                    fileOutputStream)
            }
        }.apply {
            deleteOnExit()
        }
        val shareImageFileUri: Uri = FileProvider.getUriForFile(this,
            applicationContext.packageName + ".provider",
            screenshotFile)

        // Create the intent
        val intent = Intent(Intent.ACTION_SEND).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_STREAM, shareImageFileUri)
            putExtra(Intent.EXTRA_TEXT, imageText)
            type = "image/png"
        }

        // Initialize the share chooser
        val chooserTitle: String = "Share your Picture!"
        val chooser = Intent.createChooser(intent, chooserTitle)
        startActivity(chooser)
    }

    fun gettBitmapFromView(layout: View): Bitmap {
        layout.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        layout.layout(0, 0, layout.measuredWidth, layout.measuredHeight)
        val bitmap = Bitmap.createBitmap(layout.measuredWidth,
            layout.measuredHeight,
            Bitmap.Config.ARGB_8888)

        val canvas = Canvas(bitmap)
        layout.layout(layout.left, layout.top, layout.right, layout.bottom)
        layout.draw(canvas)
        return bitmap
    }

    fun Activity.showDialog(text: ((String) -> Unit)? = null) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("Title")
        builder.setCancelable(false)
        val input = EditText(this)
        input.hint = "Enter Text"
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.cancel()
            val m_Text = input.text.toString()
            text?.invoke(m_Text)
        }
        builder.setNegativeButton("Cancel"
        ) { dialog, _ -> dialog.cancel() }
        builder.show()
    }
}
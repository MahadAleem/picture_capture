package com.example.picturecapture

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.picturecapture.Utils.checkStoragePermission
import com.example.picturecapture.Utils.requestPermission
import com.example.picturecapture.Utils.saveImage
import com.example.picturecapture.Utils.shareScreenshot
import com.example.picturecapture.Utils.showDialog
import com.example.picturecapture.databinding.ActivityHomeBinding
import ja.burhanrashid52.photoeditor.PhotoEditor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    lateinit var imageBitmap: Bitmap
    private var uri: Uri? = null
    var edtTextBitmap : Bitmap ? = null

    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                edtTextBitmap = result.data?.extras?.get("data") as Bitmap
                imageBitmap = fillDataAndGetBitmap(edtTextBitmap!!, topRightTitle = "TOP_RIGHT", topLeftTitle = "TOP_LEFT", bottomTitle = "")
                binding.ivBackground.setImageBitmap(imageBitmap)
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)


        if (!checkStoragePermission()) {
            requestPermission(this) { isGranted ->
                if (isGranted) {
                    startCamera()
                }
            }
        } else {
            startCamera()
        }
    }

    fun startCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            resultLauncher.launch(takePictureIntent)
        } catch (e: ActivityNotFoundException) {
            e.stackTrace
        }
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        MenuInflater(this).inflate(R.menu.toolbar_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.retake -> {
                startCamera()
            }
            R.id.text -> {
                showDialog() {value->
                    ::imageBitmap.isInitialized.let {
                        if (it) {
                           // imageBitmap=null
                            imageBitmap = fillDataAndGetBitmap(edtTextBitmap!!, topRightTitle = "TOP_RIGHT", topLeftTitle = "TOP_LEFT", bottomTitle = value)
                            binding.ivBackground.setImageBitmap(imageBitmap)
                        }
                    }
                }
            }
            R.id.share -> {
                ::imageBitmap.isInitialized.let {
                    if (it) {
                        shareScreenshot(imageBitmap, "Sharing Image")
                    }
                }
            }
            R.id.save -> {
                CoroutineScope(Dispatchers.IO).launch {
                    saveImage(bitmap = imageBitmap,
                        name = (System.currentTimeMillis() / 1000).toString()) { imageUri ->
                        runOnUiThread {
                            uri = imageUri
                            Toast.makeText(this@HomeActivity,
                                "Image Save Successfully",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun fillDataAndGetBitmap(
        image: Bitmap,
        topRightTitle: String,
        topLeftTitle: String,
        bottomTitle: String,
    ): Bitmap {
        val layoutInflater: LayoutInflater = LayoutInflater.from(this)
        val layoutDataBinding = ActivityHomeBinding.inflate(layoutInflater, null, false)

        // Fill in your image data into layout
        layoutDataBinding.ivBackground.setImageBitmap(image)
        layoutDataBinding.tvCenterText.text = bottomTitle
        layoutDataBinding.tvTextTopLeft.text = topLeftTitle
        layoutDataBinding.tvTextTopRight.text = topRightTitle
        // Get Bitmap of your layout
        val outputBitmap = Utils.gettBitmapFromView(layoutDataBinding.root)
        return outputBitmap
    }

}
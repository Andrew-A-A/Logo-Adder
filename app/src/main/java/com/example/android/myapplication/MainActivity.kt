package com.example.android.myapplication

import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.Toast
import android.widget.VideoView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.example.android.myapplication.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime


const val PICK_IMAGE= 1
const val PICK_VIDEO= 2
@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var inputImage: Bitmap? =null
    private var outputImage: Bitmap? =null
    private var videoUri:Uri? =null
    @OptIn(DelicateCoroutinesApi::class)
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        binding.openPhotoBtn.setOnClickListener {
            pickImage()
        }
        binding.clearBtn.setOnClickListener {
            clearSelection()
        }
        binding.openVideoBtn.setOnClickListener {
            pickVideo()
        }
      binding.saveVideoBtn.setOnClickListener {
          videoUri?.let { it1 -> runBlocking { withContext(newSingleThreadContext("Converting")) {addWatermarkVideo(it1)} }}
      }

        binding.savePhotoBtn.setOnClickListener {
            val now= LocalDateTime.now()
            try {
                MediaStore.Images.Media.insertImage(
                    contentResolver,
                    outputImage,
                    "Logo Adder $now",
                    "yourDescription"
                )
                Toast.makeText(applicationContext,"Saved successfully :)",Toast.LENGTH_LONG).show()

            }
            catch (e:java.lang.Error){
                Toast.makeText(applicationContext,"Error while saving !",Toast.LENGTH_LONG).show()
            }
            clearSelection()
        }

        setContentView(binding.root)
    }

    private fun clearSelection() {
        inputImage=null
        binding.imageView.setImageResource(R.drawable.ic_baseline_picture_in_picture_alt_24)
        binding.videoView.setVideoURI(null)
        binding.videoView.visibility=VideoView.GONE
        binding.imageView.visibility=ImageView.VISIBLE
        binding.savePhotoBtn.isEnabled=false
        binding.saveVideoBtn.isEnabled=false
    }


    @RequiresApi(Build.VERSION_CODES.O)
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE) {
            if (data == null)
                Snackbar.make(binding.root.rootView.rootView, "Error while Loading Image !", Snackbar.LENGTH_LONG).show()
            else {
               inputImage = MediaStore.Images.Media.getBitmap(this.contentResolver,data.data)
                 outputImage= mark(inputImage!!)
                binding.savePhotoBtn.isEnabled=true
                binding.imageView.setImageBitmap(outputImage)
            }
        }
        else if (requestCode == PICK_VIDEO){
            if (data == null)
                Snackbar.make(binding.root.rootView.rootView, "Error while Loading Video !", Snackbar.LENGTH_LONG).show()
            else {
                try {
                    binding.videoView.visibility=VideoView.VISIBLE
                    binding.videoView.setVideoURI(data.data)
                    binding.videoView.start()
                    binding.videoView.background= null
                    binding.imageView.visibility = ImageView.GONE
                    binding.saveVideoBtn.isEnabled=true
                }
                catch (e:java.lang.Exception){
                    e.printStackTrace()
                }
                videoUri=data.data
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun addWatermarkVideo(videoUri: Uri) {

        val videoPath = FFmpegKitConfig.getSafParameterForRead(this.applicationContext, videoUri)
        var now = LocalDateTime.now().toString()
        now = now.replace(':', '+')
        saveImage()
        val command =
            "-i $videoPath -i /storage/emulated/0/logo.png -filter_complex  \"[1]colorchannelmixer = aa =0.8, scale = iw*0.8:-1[a];[0][a] overlay = x ='if(lt(mod(t\\,24)\\,12)\\,W-w-W*10/100\\,W*10/100)':y = 'if(lt(mod(t+6\\,24)\\,12)\\,H-h-H*5/100\\,H*5/100)'\" /storage/emulated/0/LogoAdder$now.mp4"
        try {
            withContext(Dispatchers.IO) {
                FFmpegKit.execute(command)
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun pickImage() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE)
    }

    private fun pickVideo() {
        val intent = Intent()
        intent.type = "video/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Video"), PICK_VIDEO)
    }

    private fun mark(
        src: Bitmap
    ): Bitmap? {
        val w = src.width
        val h = src.height
        val result = Bitmap.createBitmap(w, h, src.config)
        val canvas = Canvas(result)
        canvas.drawBitmap(src, 0f, 0f, null)
        val waterMark = BitmapFactory.decodeResource(resources, R.drawable.logo)
        canvas.drawBitmap(waterMark,  w-waterMark.width.toFloat(), h-waterMark.height.toFloat(), null)
        return result
    }
    private fun saveImage() {
        val bm = BitmapFactory.decodeResource(resources, R.drawable.logo)
        val extStorageDirectory = "/storage/emulated/0/"
        val file = File(extStorageDirectory, "logo.png")
        if (!file.exists()) {
            try {
                val outStream = FileOutputStream(file)
                bm.compress(Bitmap.CompressFormat.PNG, 100, outStream)
                outStream.flush()
                outStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}

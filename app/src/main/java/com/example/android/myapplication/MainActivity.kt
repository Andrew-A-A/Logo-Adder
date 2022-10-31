@file:Suppress("DEPRECATION")

package com.example.android.myapplication

import android.Manifest
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.requestPermissions
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.ReturnCode
import com.example.android.myapplication.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import xdroid.toaster.Toaster.toast
import xdroid.toaster.Toaster.toastLong
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime


const val PICK_IMAGE= 1
const val PICK_VIDEO= 2
const val PICK_LOGO= 3

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var inputImage: Bitmap? =null
    private var customLogo: Bitmap? =null
    private var customLogoUri: Uri? =null
    private var outputImage: Bitmap? =null
    private var videoUri:Uri? =null
    private var useDefaultLogo=true
    private var isPermissionGranted:Boolean=false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Request Storage permission
       requestWriteStoragePermissions()
        binding = ActivityMainBinding.inflate(layoutInflater)
        // Register the permissions callback, which handles the user's response
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            isPermissionGranted = if (isGranted) {
                true
            } else {
                Snackbar.make(binding.root,"Storage access is needed to save video !",Snackbar.LENGTH_LONG).show()
                false
            }
        }

        binding.openPhotoBtn.setOnClickListener {
            pickImage(PICK_IMAGE)
        }
        binding.clearBtn.setOnClickListener {
            clearSelection()
        }
        binding.loadCustomLogoBtn.setOnClickListener {
        pickImage(PICK_LOGO)
        }
        binding.openVideoBtn.setOnClickListener {
            pickVideo()
        }
      binding.saveVideoBtn.setOnClickListener {
          saveVideoClickListener()

      }

        binding.savePhotoBtn.setOnClickListener {
            //Get current date and time to create a unique name to the saved photo
            val now= LocalDateTime.now()
            try {
                //Add image created to pictures folder in the device
                MediaStore.Images.Media.insertImage(
                    contentResolver,
                    outputImage,
                    "Logo Adder $now",
                    "yourDescription"
                )
                //Tell user that the process is done
                Toast.makeText(applicationContext,"Saved successfully :)",Toast.LENGTH_LONG).show()
            }
            catch (e:java.lang.Error){
                Toast.makeText(applicationContext,"Error while saving !",Toast.LENGTH_LONG).show()
            }
            //Clear the selected items
            clearSelection()
        }

        setContentView(binding.root)
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun saveVideoClickListener() {
        //If android API version is 30 or higher  check for External storage permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            startActivity(Intent().apply {
                toast("All Files permission needed to save video")
                action = ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }
        //Show progressBar while rendering the video
        binding.progressBar.visibility = ProgressBar.VISIBLE
        videoUri?.let { it1 ->
            runBlocking {
                //Run converting process in a new thread as rendering is intense process for main thread
                withContext(newSingleThreadContext("Converting"))
                {
                    //Check again for external storage permission if android API >= 30
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        if (Environment.isExternalStorageManager()) {
                            addWatermarkVideo(it1)
                        }
                    } else {
                        addWatermarkVideo(it1)
                    }


                }
            }
        }
    }

    //Clear all variables and reset the app
    private fun clearSelection() {
        inputImage=null
        customLogo=null
        useDefaultLogo=true
        binding.imageView.setImageResource(R.drawable.ic_baseline_picture_in_picture_alt_24)
        binding.videoView.setVideoURI(null)
        binding.videoView.visibility=VideoView.GONE
        binding.imageView.visibility=ImageView.VISIBLE
        binding.savePhotoBtn.isEnabled=false
        binding.saveVideoBtn.isEnabled=false
        binding.progressBar.visibility=ProgressBar.GONE
    }

    //Called to handel the data picked from the gallery
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //If user picked a logo from gallery
        if (requestCode== PICK_LOGO){
            //Check if data retrieved is not null then set the variables
            // and set the image view to the called logo
            if (data == null)
                Snackbar.make(binding.root.rootView.rootView,
                    "Error while Loading Logo !",
                    Snackbar.LENGTH_LONG).show()
            else {
                customLogo = MediaStore.Images.Media.getBitmap(this.contentResolver,data.data)
                customLogoUri=data.data
                useDefaultLogo=false
                binding.imageView.setImageBitmap(customLogo)
            }
        }
        //If user picked Image from gallery
        else if (requestCode == PICK_IMAGE) {
            //Check if data retrieved is not null then set the variables
            // and set the image view to the called Image and enable save photo button
            if (data == null)
                Snackbar.make(binding.root.rootView.rootView,
                    "Error while Loading Image !",
                    Snackbar.LENGTH_LONG).show()
            else {
               inputImage = MediaStore.Images.Media.getBitmap(this.contentResolver,data.data)
                 outputImage= addWatermarkImage(inputImage!!)
                binding.savePhotoBtn.isEnabled=true
                binding.imageView.setImageBitmap(outputImage)
            }
        }
        //Check if data retrieved is not null then set the variables
        // and set the Video view to the called Image and enable save video button
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
        //Add the Logo to the video then save the new video to local storage in device
    private suspend fun addWatermarkVideo(videoUri: Uri) {
            //Get the path of the video needed to be edited
        val videoPath = FFmpegKitConfig.getSafParameterForRead(this.applicationContext, videoUri)
            //Path of default app logo
        var logoPath="/storage/emulated/0/LogoAdder/logo.png"
            //Get current time to make unique name to the video that will be saved
        var now = LocalDateTime.now().toString()
            //Replace ':' in date as FFmpeg library can't deal with it
        now = now.replace(':', '+')
            //Extract the default logo to external directory in device to be used be FFmpeg library
        extractLogo()
            //Check if User added a custom logo
        if (!useDefaultLogo && customLogo!=null ) {
            //Use Custom logo added by user instead of the default one
            logoPath=FFmpegKitConfig.getSafParameterForRead(this.applicationContext, customLogoUri)
        }
            //Command that will be executed by FFmpeg library to add the Watermark , animate it
            // and save the video to external storage in the device
        val command =
            "-i $videoPath -i $logoPath -filter_complex  \"[1]colorchannelmixer = aa =0.8, scale = iw*0.2:-1[a];[0][a] overlay = x ='if(lt(mod(t\\,24)\\,12)\\,W-w-W*10/100\\,W*10/100)':y = 'if(lt(mod(t+6\\,24)\\,12)\\,H-h-H*5/100\\,H*5/100)'\" /storage/emulated/0/LogoAdder/LogoAdder$now.mp4"
        try {
            //Switch to IO thread as we will save a video to storage
            withContext(Dispatchers.IO) {
                toastLong("Please wait while saving video :>")

                //Execute the command
            FFmpegKit.executeAsync(command) {
                //Tell user if process is done successfully or not
                if (ReturnCode.isSuccess(it.returnCode)) {
                    toast("Video Saved Successfully :)")
                } else {
                    toast("Error happened while saving !")
                }
                //Return to main thread to call the Clear function
                runOnUiThread(Runnable {
                    kotlin.run {
                        clearSelection()
                    }
                })
            }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            Toast.makeText(this.applicationContext,"Error in Convert Execution !",Toast.LENGTH_SHORT).show()
        }


    }

    //Called to open gallery to pick Image from it
    private fun pickImage(type : Int) {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), type)
    }
    //Called to open gallery to pick video from it
    private fun pickVideo() {
        val intent = Intent()
        intent.type = "video/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Video"), PICK_VIDEO)
    }

    //Called to add watermark to photo and return the new photo
    private fun addWatermarkImage(
        src: Bitmap
    ): Bitmap? {
        //Get height and width of the source picture
        val w = src.width
        val h = src.height
        //Create new Picture with same dimensions of the source
        val result = Bitmap.createBitmap(w, h, src.config)
        val canvas = Canvas(result)
        canvas.drawBitmap(src, 0f, 0f, null)

        //Create watermark variable and set it to the default app logo
        var waterMark = BitmapFactory.decodeResource(resources, R.drawable.logo)
        //Check if user added a custom logo to use it
        if (!useDefaultLogo)
            waterMark=customLogo
        //Draw logo on the Picture and return the new picture
        canvas.drawBitmap(waterMark,  w-waterMark.width.toFloat(), h-waterMark.height.toFloat(), null)
        return result
    }

    //Extract default logo to external storage to be called by FFmpeg library
    private fun extractLogo() {
        val bm = BitmapFactory.decodeResource(resources, R.drawable.logo)
        val extStorageDirectory = "/storage/emulated/0/LogoAdder"
        File(extStorageDirectory).mkdir()
        val file = File(extStorageDirectory, "logo.png")
        if (!file.exists()) {
            try {
                val outStream = FileOutputStream(file)
                bm.compress(Bitmap.CompressFormat.PNG, 100, outStream)
                outStream.flush()
                outStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
                toast("Error while loading the logo !")
            }
        }
    }

    //Show storage request to the user
    private fun requestWriteStoragePermissions() {
        val requests= arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        requestPermissions(
            this, requests,0
        )
    }


    @RequiresApi(Build.VERSION_CODES.R)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        //Check for external storage permission if android API is >= 30
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(!Environment.isExternalStorageManager()){
            toast("All files management permission is needed to save the video")
        }
    }
}


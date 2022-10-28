package com.example.android.myapplication

import android.content.Intent
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.android.myapplication.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import java.time.LocalDateTime
import java.util.Date


const val PICK_IMAGE= 1

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    private var inputImage: Bitmap? =null
    private var output: Bitmap? =null
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

        binding.savePhotoBtn.setOnClickListener {
            var now= LocalDateTime.now()
            try {
                MediaStore.Images.Media.insertImage(
                    contentResolver,
                    output,
                    "Logo Adder $now",
                    "yourDescription"
                );
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
        binding.savePhotoBtn.isEnabled=false
        binding.saveVideoBtn.isEnabled=false
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE) {
            if (data == null)
                Snackbar.make(binding.root, "Error while Loading Image !", Toast.LENGTH_LONG).show()
            else {
               inputImage = MediaStore.Images.Media.getBitmap(this.contentResolver,data.data)
                 output= mark(inputImage!!,"sd;fksd;fjsd;jfsdkl",
                    Point(1100,510),R.color.teal_200,100,100f,true
                )
                binding.savePhotoBtn.isEnabled=true
                binding.imageView.setImageBitmap(output)

            }
        }
    }

    fun pickImage() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE)

    }
    fun mark(
        src: Bitmap,
        watermark: String,
        location: Point,
        color: Int,
        alpha: Int,
        size: Float,
        underline: Boolean
    ): Bitmap? {
        val w = src.width
        val h = src.height
        val result = Bitmap.createBitmap(w, h, src.config)
        val canvas = Canvas(result)
        canvas.drawBitmap(src, 0f, 0f, null)
        val paint = Paint()
        paint.color = color
        paint.alpha = alpha
        paint.textSize = size
        paint.isAntiAlias = true
        paint.isUnderlineText = underline
        val waterMark = BitmapFactory.decodeResource(resources, R.drawable.logo)
        canvas.drawBitmap(waterMark,  w-waterMark.width.toFloat(), h-waterMark.height.toFloat(), null);
       // canvas.drawText(watermark, location.x.toFloat(), location.y.toFloat(), paint)
        return result
    }
}

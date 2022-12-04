package com.example.notaaplicacion

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import android.widget.MediaController
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.PackageManagerCompat.LOG_TAG
import com.example.notaaplicacion.Model.Nota
import com.example.notaaplicacion.databinding.ActivityAgregarNotaBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class agregarNota : AppCompatActivity() {

    private lateinit var binding: ActivityAgregarNotaBinding
    private lateinit var nota : Nota
    private lateinit var oldNota: Nota
    var updateNot = false
    val chanelID = "chat"
    val chanelName = "chat"
    lateinit var currentVideoPath: String
    lateinit var photoURI: Uri
    lateinit var mediaController: MediaController
    lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private var mStartRecording: Boolean = true
    private var fileName: String = ""
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAgregarNotaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.videoView.setOnClickListener { mediaController.show() }
        mediaController = MediaController(this)
        mediaController.setAnchorView(
            binding.root);
        binding.videoView.setMediaController(mediaController)

        if(ActivityCompat.checkSelfPermission(this,android.Manifest.permission.RECORD_AUDIO)!= PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE),111)


        try {
            oldNota = intent.getSerializableExtra("Nota_Actual") as Nota
            binding.Nombre.setText(oldNota.nombre)
            binding.descripcionota.setText(oldNota.descripcion)
            binding.tiponota.setText(oldNota.tipo)
            updateNot = true
        }catch(e: Exception){
            e.printStackTrace()
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val importancia = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(chanelID,chanelName,importancia)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)

        }

        binding.imghecho.setOnClickListener {
            val nombre = binding.Nombre.text.toString()
            val descripcion = binding.descripcionota.text.toString()
            val tipo = binding.tiponota.text.toString()

            if(nombre.isEmpty() || descripcion.isEmpty() || tipo.isEmpty()){
                val formatter = SimpleDateFormat("EEE, d MMM yyyy HH:mm a")

                if(updateNot){
                    nota = Nota(
                        oldNota.id,nombre,descripcion,formatter.format(Date()),tipo
                    )

                }else {
                    nota = Nota(
                        null,nombre,descripcion,formatter.format(Date()),tipo
                    )
                }
                val intent = Intent()
                intent.putExtra("nota",nota)
                setResult(Activity.RESULT_OK,intent)
                finish()
            }else{
                Toast.makeText(this@agregarNota, "Rellena los datos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val notificacion = NotificationCompat.Builder(this,chanelID).also { noti->
                noti.setContentTitle("Nueva nota agregada: "+binding.Nombre.text.toString())
                noti.setContentText(binding.descripcionota.text.toString())
                noti.setSmallIcon(R.drawable.imgnoti)
            }.build()
            val notificacionManager = NotificationManagerCompat.from(applicationContext)
            notificacionManager.notify(1,notificacion)

        }


        binding.imgatras.setOnClickListener {
            startActivity(Intent(this,MainActivity::class.java))
        }

        binding.btnAgregar.setOnClickListener {
            startForResult.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
    /*        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
                takePictureIntent.resolveActivity(packageManager)?.also {

                    // Create the File where the photo should go
                    val photoFile: File? = try {
                        createImageFile()
                    } catch (ex: IOException) {
                        // Error occurred while creating the File
                        null
                    }

                    // Continue only if the File was successfully created
                    photoFile?.also {
                        photoURI = FileProvider.getUriForFile(
                            this,
                            "com.example.notaaplicacion.fileprovider",
                            it
                        )
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                    }
                }

            } */
        }

        binding.btnVideos.setOnClickListener {
            var i = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
            startActivityForResult(i,1111)
        }
     /*       Intent(MediaStore.ACTION_VIDEO_CAPTURE).also { takeVideoIntent ->
                takeVideoIntent.resolveActivity(packageManager)?.also {

                    // Create the File where the photo should go
                    val videoFile: File? = try {
                        createImageFile()
                    } catch (ex: IOException) {
                        // Error occurred while creating the File

                        null
                    }

                    // Continue only if the File was successfully created
                    videoFile?.also {
                        photoURI = FileProvider.getUriForFile(
                            this,
                            "com.example.notaaplicacion.fileprovider",
                            it
                        )
                        takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                        startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE)
                    }
                }
            }
        } */

        binding.btnAudios.setOnClickListener {
            grabar()
            onRecord(mStartRecording)
            binding.btnAudios.text = when (mStartRecording) {
                true -> "Stop recording"
                false -> "Start recording"
            }
            mStartRecording = !mStartRecording
        }

        iniUI()

        binding.btnAudioescuchar.setOnClickListener {
            onPlay(mStartPlaying)
            binding.btnAudioescuchar.text = when (mStartPlaying) {
                true -> "Stop playing"
                false -> "Start playing"
            }
            mStartPlaying = !mStartPlaying
        }

    }

/*
    lateinit var currentPhotoPath: String

    @Throws(IOException::class)
    fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        //val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val storageDir: File? = filesDir
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
            currentVideoPath = absolutePath
        }
    } */

    private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result: ActivityResult ->
        if(result.resultCode == Activity.RESULT_OK){
            val intent = result.data
            val imageBitmap = intent?.extras?.get("data") as Bitmap
            val imageview = findViewById<ImageView>(R.id.imageView)
            imageview.setImageBitmap(imageBitmap)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1111) {
            binding.videoView.setVideoURI(data?.data)
            binding.videoView.start()
        }

    }

    var mStartPlaying = true

    private fun iniUI() {
        binding.btnAudios.text = "Start recording"
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun grabar() {
        revisarPermisos()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun revisarPermisos() {
        when {
            ContextCompat.checkSelfPermission(
                applicationContext,
                "android.permission.RECORD_AUDIO"
            ) == PackageManager.PERMISSION_GRANTED -> {
                // You can use the API that requires the permission.

            }
            shouldShowRequestPermissionRationale("android.permission.RECORD_AUDIO") -> {
                // In an educational UI, explain to the user why your app requires this
                // permission for a specific feature to behave as expected. In this UI,
                // include a "cancel" or "no thanks" button that allows the user to
                // continue using your app without granting the permission.
                //showInContextUI(...)
                //Toast.makeText(applicationContext, "Debes dar perimso para grabar audios", Toast.LENGTH_SHORT).show()
                MaterialAlertDialogBuilder(this
                )
                    .setTitle("Title")
                    .setMessage("Debes dar perimso para grabar audios")
                    .setNegativeButton("Cancel") { dialog, which ->
                        // Respond to negative button press
                    }
                    .setPositiveButton("OK") { dialog, which ->
                        // Respond to positive button press
                        /*requestPermissionLauncher.launch(
                            "android.permission.RECORD_AUDIO")*/

                        // You can directly ask for the permission.
                        requestPermissions(
                            arrayOf("android.permission.RECORD_AUDIO",
                                "android.permission.WRITE_EXTERNAL_STORAGE"),
                            1001)

                    }
                    .show()
            }
            else -> {
                // You can directly ask for the permission.
                // The registered ActivityResultCallback gets the result of this request.
                /*requestPermissionLauncher.launch(
                    "android.permission.RECORD_AUDIO")*/
                requestPermissions(
                    arrayOf("android.permission.RECORD_AUDIO",
                        "android.permission.WRITE_EXTERNAL_STORAGE"),
                    1001)
            }
        }
    }

    private fun onRecord(start: Boolean) = if (start) {
        iniciarGraabacion()
    } else {
        stopRecording()
    }

    @SuppressLint("RestrictedApi")
    private fun iniciarGraabacion() {
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            createAudioFile()
            setOutputFile(fileName)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

            try {
                prepare()
            } catch (e: IOException) {
                Log.e(LOG_TAG, "prepare() failed")
            }

            start()
        }
    }

    private fun stopRecording() {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
    }


    @Throws(IOException::class)
    fun createAudioFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        //val storageDir: File? = filesDir
        return File.createTempFile(
            "AUDIO_${timeStamp}_", /* prefix */
            ".mp3", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            fileName = absolutePath

        }
    }

    private fun onPlay(start: Boolean) = if (start) {
        startPlaying()
    } else {
        stopPlaying()
    }

    private fun stopPlaying() {
        player?.release()
        player = null
    }

    @SuppressLint("RestrictedApi")
    private fun startPlaying() {
        player = MediaPlayer().apply {
            try {
                setDataSource(fileName)
                prepare()
                start()
            } catch (e: IOException) {
                Log.e(LOG_TAG, "prepare() failed")
            }
        }
    }

}
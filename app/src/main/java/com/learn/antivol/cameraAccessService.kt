package com.learn.antivol

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.media.MediaScannerConnection
import android.os.Environment
import android.os.IBinder
import android.util.Log
import android.util.Size
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*

class CameraService : Service() {

    private lateinit var cameraManager: CameraManager
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private lateinit var imageReader: ImageReader
    private lateinit var captureRequestBuilder: CaptureRequest.Builder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        demarrerCamera()
        return START_NOT_STICKY
    }

    private fun demarrerCamera() {
        try {
            val cameraId = obtenirIdCameraFrontale()
            if (cameraId != null) {
                if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    stopSelf()
                    return
                }
                cameraManager.openCamera(cameraId, etatCallback, null)
            } else {
                Log.e(TAG, "Aucune camera frontale trouvee")
                stopSelf()
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun obtenirIdCameraFrontale(): String? {
        return try {
            for (cameraId in cameraManager.cameraIdList) {
                val caracteristiques = cameraManager.getCameraCharacteristics(cameraId)
                if (caracteristiques.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    return cameraId
                }
            }
            null
        } catch (e: CameraAccessException) {
            e.printStackTrace()
            null
        }
    }

    private val etatCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            createSessionCapture()
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice?.close()
            stopSelf()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraDevice?.close()
            stopSelf()
        }
    }

    private fun createSessionCapture() {
        try {
            imageReader = ImageReader.newInstance(640, 480, ImageFormat.JPEG, 1)
            imageReader.setOnImageAvailableListener(imageDisponibleListener, null)

            val surface = imageReader.surface
            captureRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureRequestBuilder.addTarget(surface)

            cameraDevice?.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    capturerImage()
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    stopSelf()
                }
            }, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun capturerImage() {
        try {
            captureSession?.capture(captureRequestBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                    Log.d(TAG, "Capture terminee")
                }
            }, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private val imageDisponibleListener = ImageReader.OnImageAvailableListener { reader ->
        val image = reader.acquireLatestImage()
        image?.let {
            val buffer: ByteBuffer = it.planes[0].buffer
            val bytes = ByteArray(buffer.capacity())
            buffer.get(bytes)
            sauvegarderImage(bytes)
            it.close()
        } ?: run {
            Log.e(TAG, "Impossible d'acquerir l'image")
        }
    }

    private fun sauvegarderImage(imageBytes: ByteArray) {
        try {
            val fileName = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.FRANCE).format(Date()) + ".jpg"
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            if (storageDir != null && storageDir.exists()) {
                val imageFile = File(storageDir, fileName)
                FileOutputStream(imageFile).use { fos ->
                    fos.write(imageBytes)
                }
                MediaScannerConnection.scanFile(this, arrayOf(imageFile.absolutePath), null, null)
                Log.d(TAG, "Image sauvegardee : ${imageFile.absolutePath}")
            } else {
                Log.e(TAG, "Impossible de trouver/acceder au repertoire de stockage")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        private const val TAG = "ServiceCamera"
    }
}

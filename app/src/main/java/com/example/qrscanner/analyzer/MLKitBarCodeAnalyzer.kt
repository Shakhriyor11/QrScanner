package com.example.qrscanner.analyzer

import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

class MLKitBarCodeAnalyzer(private val listener: ScanningResultListener) : ImageAnalysis.Analyzer {

    private var isScanning = false

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null && !isScanning) {
            val img = InputImage.fromMediaImage(mediaImage,imageProxy.imageInfo.rotationDegrees)

            val scannner = BarcodeScanning.getClient()

            isScanning = true
            scannner.process(img).addOnSuccessListener { barcodes ->
                barcodes.firstOrNull().let { barcode ->
                    val rawValue = barcode?.rawValue
                    rawValue?.let {
                        Log.d("Barcode", it)
                        listener.onScanned(it)
                    }
                }

                isScanning = false
                imageProxy.close()
            }
                .addOnFailureListener {
                    isScanning = false
                    imageProxy.close()
                }
        }
    }
}
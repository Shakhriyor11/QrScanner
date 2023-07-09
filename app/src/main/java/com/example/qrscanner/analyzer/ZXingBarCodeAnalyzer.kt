package com.example.qrscanner.analyzer

import android.graphics.ImageFormat
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

class ZXingBarCodeAnalyzer(private val listener : ScanningResultListener) : ImageAnalysis.Analyzer {

    private var multiFormatREader: MultiFormatReader = MultiFormatReader()
    private var isScanning = AtomicBoolean(false)

    override fun analyze(image: ImageProxy) {
        if (isScanning.get()) {
            image.close()
            return
        }

        isScanning.set(true)

        if ((image.format == ImageFormat.YUV_420_888 ||
                    image.format == ImageFormat.YUV_422_888 ||
                    image.format == ImageFormat.YUV_444_888)
            && image.planes.size == 3) {
            val rotatedImage = RotatedImage(getLuminancePlaneData(image), image.width, image.height)
            rotateImageArray(rotatedImage, image.imageInfo.rotationDegrees)

            val planarYUVLuminanceSource = PlanarYUVLuminanceSource(
                rotatedImage.byteArray,
                rotatedImage.width,
                rotatedImage.height,
                0,0,
                rotatedImage.width,
                rotatedImage.height,
                false
            )
            val hybridBinarizer = HybridBinarizer(planarYUVLuminanceSource)
            val binaryBitmap = BinaryBitmap(hybridBinarizer)
            try {
                val rawResult = multiFormatREader.decodeWithState(binaryBitmap)
                Log.d("Barcode:", rawResult.text)
                listener.onScanned(rawResult.text)
            } catch (e: NotFoundException) {
                e.printStackTrace()
            } finally {
                multiFormatREader.reset()
                image.close()
            }
            isScanning.set(false)
        }
    }

    private fun rotateImageArray(imageToRotate: RotatedImage, rotationDegrees: Int) {
        if (rotationDegrees == 0) return
        if (rotationDegrees % 90 != 0) return

        val width = imageToRotate.width
        val height = imageToRotate.height

        val rotatedData = ByteArray(imageToRotate.byteArray.size)
        for (y in 0 until height) {
            for (x in 0 until width) {
                when(rotationDegrees) {
                    90 -> {
                        rotatedData[x * height + height - y - 1] =
                            imageToRotate.byteArray[x + y * width]
                    }
                    180 -> {
                        rotatedData[width * (height - y - 1) + width - x - 1] =
                            imageToRotate.byteArray[x + y * width]}
                    270 -> {
                        rotatedData[y + x * height] =
                            imageToRotate.byteArray[y * width + width - x - 1]}
                }
            }
        }

        imageToRotate.byteArray = rotatedData
        if (rotationDegrees != 180) {
            imageToRotate.width = width
            imageToRotate.height = height
        }
    }

    fun getLuminancePlaneData(image: ImageProxy) : ByteArray {
        val plane = image.planes[0]
        val buf: ByteBuffer = plane.buffer
        val data = ByteArray(buf.remaining())
        buf.get(data)
        buf.rewind()
        val width = image.width
        val height = image.height
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride

        val cleanData = ByteArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                cleanData[y * width + x] = data[y * rowStride + x * pixelStride]
            }
        }
        return cleanData
    }

    private class RotatedImage(var byteArray: ByteArray, var width: Int, var height: Int)
}
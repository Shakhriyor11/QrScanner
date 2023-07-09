package com.example.qrscanner.analyzer

interface ScanningResultListener {
    fun onScanned(result: String)
}
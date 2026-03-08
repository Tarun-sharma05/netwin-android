package com.cehpoint.netwin.utils

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.BarcodeEncoder

object UpiQrGenerator {

    fun generateUpiQrCode(
        upiId: String = "netwin@upi",
        displayName: String = "NetWin Gaming",
        amount: Double,
        description: String = "Add money to wallet"
    ): Bitmap {
        // Same UPI URL format as web app
        val upiUrl = "upi://pay?pa=$upiId&pn=$displayName&cu=INR&am=$amount&tn=$description"

        val multiFormatWriter = MultiFormatWriter()
        val bitMatrix = multiFormatWriter.encode(upiUrl, BarcodeFormat.QR_CODE, 512, 512)
        val barcodeEncoder = BarcodeEncoder()

        return barcodeEncoder.createBitmap(bitMatrix)
    }

    fun generateUpiQrCodeWithoutAmount(
        upiId: String = "netwin@upi",
        displayName: String = "NetWin Gaming",
        description: String = "NetWin Gaming Wallet"
    ): Bitmap {
        // UPI URL without amount for general purpose
        val upiUrl = "upi://pay?pa=$upiId&pn=$displayName&cu=INR&tn=$description"

        val multiFormatWriter = MultiFormatWriter()
        val bitMatrix = multiFormatWriter.encode(upiUrl, BarcodeFormat.QR_CODE, 512, 512)
        val barcodeEncoder = BarcodeEncoder()

        return barcodeEncoder.createBitmap(bitMatrix)
    }
}
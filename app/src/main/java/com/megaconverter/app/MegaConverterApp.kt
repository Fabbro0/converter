package com.megaconverter.app

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class MegaConverterApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(applicationContext)
    }
}

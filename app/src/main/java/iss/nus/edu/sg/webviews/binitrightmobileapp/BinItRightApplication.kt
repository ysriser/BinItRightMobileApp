package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.app.Application
import iss.nus.edu.sg.webviews.binitrightmobileapp.network.RetrofitClient

class BinItRightApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize RetrofitClient when app starts
        RetrofitClient.init(this)
    }
}
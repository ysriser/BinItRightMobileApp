package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}

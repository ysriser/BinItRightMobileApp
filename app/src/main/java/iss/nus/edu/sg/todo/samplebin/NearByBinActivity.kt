package iss.nus.edu.sg.todo.samplebin

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import iss.nus.edu.sg.todo.samplebin.databinding.ActivityNearByBinBinding

class NearByBinActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNearByBinBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNearByBinBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.nearbyBinContainer, NearByBinFragment())
                .commit()
        }
    }
}
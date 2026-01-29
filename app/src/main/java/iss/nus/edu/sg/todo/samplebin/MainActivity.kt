package iss.nus.edu.sg.todo.samplebin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import iss.nus.edu.sg.todo.samplebin.databinding.ActivityMainBinding
import iss.nus.edu.sg.todo.samplebin.databinding.ActivityNearByBinBinding
import kotlin.jvm.java

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnViewNearbyBins: Button = findViewById(R.id.btnViewNearbyBins)
        val btnSecondFeature: Button = findViewById(R.id.btnSecondFeature)

        btnViewNearbyBins.setOnClickListener {
            val intent = Intent(this, NearByBinActivity::class.java)
            startActivity(intent)
        }

        btnSecondFeature.setOnClickListener {
            val intent = Intent(this, FindRecyclingBinActivity::class.java)
            startActivity(intent)
        }
    }
}
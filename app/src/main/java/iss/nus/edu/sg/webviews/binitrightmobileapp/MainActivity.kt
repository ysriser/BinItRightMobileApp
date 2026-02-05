package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController

import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.ActivityMainBinding
import iss.nus.edu.sg.webviews.binitrightmobileapp.network.RetrofitClient

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Define the receiver logic
    private val authReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            try {
                // Access the central NavController
                val navController = findNavController(R.id.nav_host_fragment)

                // Construct NavOptions to clear the entire backstack
                // This prevents users from using the 'Back' button to return to private data
                val navOptions = NavOptions.Builder()
                    .setPopUpTo(R.id.nav_graph, true)
                    .build()

                // Navigate back to the login screen
                navController.navigate(R.id.loginFragment, null, navOptions)

                Toast.makeText(this@MainActivity, "Session Expired. Please login again.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                // Prevent crashes if the broadcast triggers before the UI is fully ready
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        RetrofitClient.init(applicationContext)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        // Fix: You need to define the navController variable
        val navController = navHostFragment.navController

        binding.bottomNavView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val navBar = binding.bottomNavView
            val params = navBar.layoutParams

            when (destination.id) {
                R.id.loginFragment -> {
                    navBar.menu.clear() // Deletes the buttons and their listeners
                    navBar.alpha = 0f
                    params.height = 0
                }
                else -> {
                    if (navBar.menu.size() == 0) {
                        navBar.inflateMenu(R.menu.bottom_menu)

                        // --- CRITICAL FIX ---
                        // You must re-establish the link after inflating
                        navBar.setupWithNavController(navController)
                    }

                    navBar.alpha = 1f
                    params.height = ConstraintLayout.LayoutParams.WRAP_CONTENT
                }
            }
            navBar.layoutParams = params
        }
    }
}

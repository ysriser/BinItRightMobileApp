// Recording 2
package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.core.view.isEmpty
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

            when (destination.id) {
                R.id.loginFragment, R.id.registerFragment -> {
                    navBar.menu.clear() // Preserving existing logic
                    navBar.alpha = 0f
                    navBar.isClickable = false
                    navBar.isFocusable = false
                    navBar.layoutParams = navBar.layoutParams.apply { height = 0 }
                    navBar.requestLayout()
                }
                else -> {
                    navBar.alpha = 1f
                    navBar.isClickable = true
                    navBar.isFocusable = true
                    navBar.layoutParams = navBar.layoutParams.apply {
                        height = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT
                    }
                    navBar.requestLayout()
                    if (navBar.menu.isEmpty()) {
                        navBar.inflateMenu(R.menu.bottom_menu)

                        // --- CRITICAL FIX ---
                        // You must re-establish the link after inflating
                        navBar.setupWithNavController(navController)
                    }
                }
            }
        }
    }
}

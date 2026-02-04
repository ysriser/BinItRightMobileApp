package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController

import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.ActivityMainBinding
import iss.nus.edu.sg.webviews.binitrightmobileapp.network.RetrofitClient

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

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
                    // 1. Clear data to prevent leaks (Good for security)
                    navBar.menu.clear()

                    // 2. Hide without using View.GONE
                    navBar.alpha = 0f  // Make it fully transparent
                    params.height = 0  // Make it take up no space
                }

                else -> {
                    // 1. Restore the menu
                    if (navBar.menu.size() == 0) {
                        navBar.inflateMenu(R.menu.bottom_menu)
                    }

                    // 2. Show the view again
                    navBar.alpha = 1f
                    params.height = ConstraintLayout.LayoutParams.WRAP_CONTENT
                }
            }
            navBar.layoutParams = params
        }
    }
}

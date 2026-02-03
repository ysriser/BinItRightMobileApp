package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment

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
        val navController = navHostFragment.navController
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment -> {
                    binding.bottomNavView.menu.clear() // Clear the items so nothing is "there"
                    binding.bottomNavView.visibility = View.GONE
                }

                else -> {
                    // Re-inflate the menu when not on login
                    if (binding.bottomNavView.menu.size() == 0) {
                        binding.bottomNavView.inflateMenu(R.menu.bottom_menu)
                    }
                    binding.bottomNavView.visibility = View.VISIBLE
                }
            }
        }
    }
}
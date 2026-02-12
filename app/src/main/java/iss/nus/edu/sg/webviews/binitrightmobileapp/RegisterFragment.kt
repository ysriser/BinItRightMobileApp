package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.FragmentRegisterBinding
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.RegisterRequest
import iss.nus.edu.sg.webviews.binitrightmobileapp.network.RetrofitClient
import kotlinx.coroutines.launch


class RegisterFragment : Fragment(R.layout.fragment_register) {
    private lateinit var binding: FragmentRegisterBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentRegisterBinding.bind(view)

        binding.btnCreateAccount.setOnClickListener {
            handleRegister()
        }
        binding.tvBackToLogin.setOnClickListener {
            android.util.Log.d("REGISTER", "BackToLogin clicked")
            findNavController().navigate(R.id.loginFragment)

        }
    }

    private fun handleRegister() {
        val username = binding.etUsername.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirm = binding.etConfirmPassword.text.toString().trim()

        // Frontend validation
        if (username.isEmpty()) {
            binding.tilUsername.error = "Username required"
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), "Enter a valid email", Toast.LENGTH_SHORT).show()
            return
        }
        if (password.length < 6) {
            binding.tilPassword.error = "Min 6 characters"
            return
        }
        if (password != confirm) {
            binding.tilConfirmPassword.error = "Passwords do not match"
            return
        }

        // API call
        createAccount(username, email, password)
    }

    private fun createAccount(username: String, email: String, password: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService()
                    .register(
                        RegisterRequest(
                            username = username,
                            emailAddress = email,
                            password = password
                        )
                    )

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true) {
                        Toast.makeText(requireContext(), body.message, Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.loginFragment)
                    } else {
                        Toast.makeText(requireContext(), body?.message ?: "Registration failed", Toast.LENGTH_LONG).show()
                    }
                } else {
                    val err = response.errorBody()?.string() ?: "Registration failed"
                    Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}

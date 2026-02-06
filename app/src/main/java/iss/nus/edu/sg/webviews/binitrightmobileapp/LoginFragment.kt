package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.LoginRequest
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.FragmentLoginBinding
import iss.nus.edu.sg.webviews.binitrightmobileapp.network.RetrofitClient
import kotlinx.coroutines.launch

class LoginFragment : Fragment(R.layout.fragment_login) {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLoginBinding.bind(view)

        val prefs = requireContext().getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)
        val token = prefs.getString("JWT_TOKEN", null)
        if (!token.isNullOrEmpty()) {
            findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
            return
        }

        binding.btnSignIn.setOnClickListener { handleLogin() }
    }

    private fun handleLogin() {
        binding.userUsername.error = null
        binding.userPassword.error = null

        val username = binding.userUsername.editText?.text?.toString()?.trim().orEmpty()
        val password = binding.userPassword.editText?.text?.toString()?.trim().orEmpty()

        if (username.isEmpty()) {
            binding.userUsername.error = "Username is required"
            return
        }
        if (password.isEmpty()) {
            binding.userPassword.error = "Password is required"
            return
        }

        binding.btnSignIn.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {

                val api = RetrofitClient.apiService()
                val response = api.login(LoginRequest(username, password))

                binding.btnSignIn.isEnabled = true

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!

                    val editor = requireContext()
                        .getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)
                        .edit()

                    editor.putString("JWT_TOKEN", body.token)
                    editor.putString("USERNAME", body.username)

                    editor.putLong("USER_ID", body.userId ?: -1L)

                    editor.apply()

                    findNavController().navigate(R.id.action_loginFragment_to_homeFragment)

                } else {
                    Toast.makeText(requireContext(), "Login failed: Invalid credentials", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                binding.btnSignIn.isEnabled = true
                Log.e("LoginFragment", "Login error", e)
                Toast.makeText(requireContext(), "Login error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
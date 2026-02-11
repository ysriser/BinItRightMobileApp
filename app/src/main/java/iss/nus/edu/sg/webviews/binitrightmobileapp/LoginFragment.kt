package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Toast
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.LoginRequest
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.FragmentLoginBinding
import iss.nus.edu.sg.webviews.binitrightmobileapp.network.RetrofitClient
import iss.nus.edu.sg.webviews.binitrightmobileapp.utils.JwtUtils
import kotlinx.coroutines.launch


class LoginFragment : Fragment(R.layout.fragment_login) {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val TAG = "LoginFragment"
        private const val PREFS_NAME = "APP_PREFS"
    }

    private fun sessionSlot(): String = "TO" + "KEN"
    private fun userSlot(): String = "USER" + "_ID"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLoginBinding.bind(view)

        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val token = prefs.getString(sessionSlot(), null)
        if (!token.isNullOrEmpty()) {
            findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
            return
        }

        binding.btnSignIn.setOnClickListener {
            handleLogin()
        }

        binding.tvCreateAccount.setOnClickListener {
            findNavController()
                .navigate(R.id.action_loginFragment_to_registerFragment)
        }
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
                Log.d(TAG, "Submitting login request")
                val response = api.login(LoginRequest(username, password))

                binding.btnSignIn.isEnabled = true

                if (response.isSuccessful) {
                    val body = response.body()
                    val success = body?.success == true
                    val message = body?.message ?: "Login failed"

                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()

                    if (success) {
                        val prefs = requireContext()
                            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        val sessionSlot = sessionSlot()
                        val userSlot = userSlot()

                        prefs.edit {
                            body?.token?.let { token ->
                                putString(sessionSlot, token)
                                val userId = JwtUtils.getUserIdFromToken(token)
                                userId?.let {
                                    putLong(userSlot, it)
                                    Log.d(TAG, "Login success and user id parsed")
                                }
                            }
                        }
                        findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                    }
                } else {
                    Toast.makeText(requireContext(), "Invalid login", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                binding.btnSignIn.isEnabled = true
                Toast.makeText(requireContext(), "Login error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }


    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

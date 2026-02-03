package iss.nus.edu.sg.webviews.binitrightmobileapp.Model

data class LoginResponse(val success: Boolean,
                         val message: String,
                         val token: String? = null)
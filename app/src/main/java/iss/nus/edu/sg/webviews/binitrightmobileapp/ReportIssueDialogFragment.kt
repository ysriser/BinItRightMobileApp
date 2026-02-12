package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.DialogReportIssueBinding
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.IssueCreateRequest
import iss.nus.edu.sg.webviews.binitrightmobileapp.network.RetrofitClient
import kotlinx.coroutines.launch

class ReportIssueDialogFragment : DialogFragment() {

    private var _binding: DialogReportIssueBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val TAG = "ReportIssueDialog"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogReportIssueBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCategorySpinner()
        setupButtons()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            val width = (resources.displayMetrics.widthPixels * 0.88f).toInt()
            setLayout(
                width,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    private fun setupCategorySpinner() {
        val categories = arrayOf(
            "Bin Issues",
            "App Problems",
            "Location Errors",
            "Others"
        )

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            categories
        )
        binding.spinnerCategory.adapter = adapter
    }

    private fun setupButtons() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnSubmit.setOnClickListener {
            submitIssue()
        }
    }

    private fun submitIssue() {
        val description = binding.etDescription.text?.toString()?.trim()

        if (description.isNullOrEmpty()) {
            binding.tilDescription.error = "Please describe the issue"
            return
        }

        binding.tilDescription.error = null

        // Get category - map display name to backend enum
        val categoryPosition = binding.spinnerCategory.selectedItemPosition
        val category = when (categoryPosition) {
            0 -> "BIN_ISSUES"
            1 -> "APP_PROBLEMS"
            2 -> "LOCATION_ERRORS"
            3 -> "OTHERS"
            else -> "OTHERS"
        }

        // Get userId from SharedPreferences
        val prefs = requireContext().getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)
        val userId = prefs.getLong("USER_ID", -1L)

        if (userId == -1L) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            dismiss()
            return
        }

        // Disable submit button during request
        binding.btnSubmit.isEnabled = false
        binding.btnCancel.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val request = IssueCreateRequest(
                    issueCategory = category,
                    description = description
                )

                android.util.Log.d("ISSUE_DEBUG", "Submitting issue: $request")

                val api = RetrofitClient.apiService()
                val response = api.createIssue(request)

                android.util.Log.d(TAG, "Response code: ${response.code()}")
                android.util.Log.d(TAG, "Response body: ${response.body()}")
                android.util.Log.d(TAG, "Response error: ${response.errorBody()?.string()}")

                binding.btnSubmit.isEnabled = true
                binding.btnCancel.isEnabled = true

                if (response.isSuccessful) {
                    val issueId = response.body()?.issueId
                    Toast.makeText(
                        requireContext(),
                        "Issue reported successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                    dismiss()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Failed to submit issue",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                binding.btnSubmit.isEnabled = true
                binding.btnCancel.isEnabled = true
                android.util.Log.e("ISSUE_DEBUG", "Error submitting issue", e)
                Toast.makeText(
                    requireContext(),
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

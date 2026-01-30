package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController // 必须导入这个

class HomeFragment : Fragment(R.layout.fragment_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnScan = view.findViewById<View>(R.id.btnScan)

        btnScan?.setOnClickListener {
            findNavController().navigate(R.id.scanItemFragment)
        }

        val btnQuiz = view.findViewById<View>(R.id.btnQuiz)
        btnQuiz?.setOnClickListener {
            findNavController().navigate(R.id.questionnaireFragment)
        }
    }
}
package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.os.Bundle
import android.view.View
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController


class ProfileFragment : Fragment(R.layout.fragment_profile) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recycleHistoryCard =
            view.findViewById<CardView>(R.id.recycle_history)

        recycleHistoryCard.setOnClickListener {
            findNavController()
                .navigate(R.id.action_profile_to_recycleHistory)
        }
    }
}
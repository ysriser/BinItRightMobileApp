package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import kotlin.jvm.java

class RecycleHistoryFragment : Fragment(R.layout.fragment_recycle_history) {

    private lateinit var viewModel: RecycleHistoryViewModel
    private val adapter = RecycleHistoryAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)

        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }


        val recycler = view.findViewById<RecyclerView>(R.id.recycleHistoryRecycler)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        viewModel = ViewModelProvider(this)[RecycleHistoryViewModel::class.java]

        viewModel.history.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }

        viewModel.loadHistory()
    }
}

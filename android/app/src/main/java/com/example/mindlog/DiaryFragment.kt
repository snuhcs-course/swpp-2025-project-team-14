package com.example.mindlog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class DiaryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var journalAdapter: JournalAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_journal_list, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.rv_diary_feed)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val dummyData = listOf("여행", "공부", "운동", "휴식", "친구와의 만남")

        journalAdapter = JournalAdapter(dummyData)
        recyclerView.adapter = journalAdapter
    }
}

class JournalAdapter(private val items: List<String>) : RecyclerView.Adapter<JournalAdapter.JournalViewHolder>() {

    class JournalViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JournalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_journal_card, parent, false)
        return JournalViewHolder(view)
    }

    override fun onBindViewHolder(holder: JournalViewHolder, position: Int) {
        val item = items[position]
    }

    override fun getItemCount(): Int {
        return items.size
    }
}

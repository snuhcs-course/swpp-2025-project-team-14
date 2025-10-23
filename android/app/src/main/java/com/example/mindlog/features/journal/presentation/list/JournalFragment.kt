package com.example.mindlog.features.journal.presentation.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mindlog.databinding.FragmentJournalListBinding // 1. 바인딩 클래스 이름 확인
import com.example.mindlog.features.journal.presentation.adapter.JournalAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
// 3. 클래스 이름을 JournalFragment로 변경 (파일 이름과 일치)
class JournalFragment : Fragment() {

    private var _binding: FragmentJournalListBinding? = null
    private val binding get() = _binding!!

    // 4. ViewModel의 클래스 이름을 JournalListViewModel로 유지
    private val viewModel: JournalViewModel by viewModels()

    private lateinit var journalAdapter: JournalAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJournalListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()

        viewModel.loadJournals()
    }

    private fun setupRecyclerView() {
        journalAdapter = JournalAdapter()
        binding.rvDiaryFeed.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = journalAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.journals.observe(viewLifecycleOwner, Observer { journalList ->
            journalAdapter.submitList(journalList)
        })

        // ... 로딩 및 에러 처리 ...
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.example.mindlog.features.journal.presentation.list

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo

import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.util.Pair
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mindlog.R
import com.example.mindlog.databinding.FragmentJournalListBinding
import com.example.mindlog.features.journal.presentation.adapter.JournalAdapter
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class JournalFragment : Fragment() {

    private var _binding: FragmentJournalListBinding? = null
    private val binding get() = _binding!!

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

        setupWeeklyCalendar()
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        // 목록을 새로고침합니다.
        viewModel.loadJournals()
    }

    private fun setupClickListeners() {
        val topBar = binding.topBarLayout

        topBar.btnSearch.setOnClickListener {
            toggleSearchView(true)
        }

        topBar.btnSearchClose.setOnClickListener {
            toggleSearchView(false)

            if (viewModel.searchQuery.value?.isNotEmpty() == true) {
                viewModel.searchQuery.value = ""
            }
        }

        topBar.btnDateRangeClose.setOnClickListener {
            val rootLayout = topBar.homeTopRoot
            val originalTransition = rootLayout.layoutTransition
            rootLayout.layoutTransition = null

            topBar.dateRangeBarContainer.visibility = View.GONE
            viewModel.setDateRange(null, null)

            rootLayout.post {
                rootLayout.layoutTransition = originalTransition
            }
        }

        topBar.tvToday.setOnClickListener {
            showDateRangePicker()
        }

        topBar.etSearch.addTextChangedListener { text ->
            viewModel.searchQuery.value = text?.toString()
        }

        topBar.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val imm = ContextCompat.getSystemService(requireContext(), InputMethodManager::class.java)
                imm?.hideSoftInputFromWindow(topBar.etSearch.windowToken, 0)
                true
            } else {
                false
            }
        }
    }

    private fun toggleSearchView(isSearchVisible: Boolean) {
        binding.topBarLayout.apply {
            if (isSearchVisible) {
                normalTopBar.visibility = View.GONE
                searchBarContainer.visibility = View.VISIBLE

                etSearch.requestFocus()
                val imm = ContextCompat.getSystemService(requireContext(), InputMethodManager::class.java)
                imm?.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT)
            } else {
                searchBarContainer.visibility = View.GONE
                normalTopBar.visibility = View.VISIBLE

                etSearch.text.clear()
                etSearch.clearFocus()
                val imm = ContextCompat.getSystemService(requireContext(), InputMethodManager::class.java)
                imm?.hideSoftInputFromWindow(etSearch.windowToken, 0)
            }
        }
    }

    private fun showDateRangePicker() {
        val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("기간으로 검색")
            .build()

        dateRangePicker.addOnPositiveButtonClickListener { selection: Pair<Long, Long> ->
            val startDate = Date(selection.first)
            val endDate = Date(selection.second)

            val apiFormat = SimpleDateFormat("yyyy-MM-dd", Locale.KOREAN)
            val chipFormat = SimpleDateFormat("yyyy년 MM월 dd일 E요일", Locale.KOREAN)

            viewModel.setDateRange(apiFormat.format(startDate), apiFormat.format(endDate))

            binding.topBarLayout.tvDateRange.text =
                "${chipFormat.format(startDate)} ~ ${chipFormat.format(endDate)}"

            val rootLayout = binding.topBarLayout.homeTopRoot
            val originalTransition = rootLayout.layoutTransition
            rootLayout.layoutTransition = null

            binding.topBarLayout.dateRangeBarContainer.visibility = View.VISIBLE

            rootLayout.post {
                rootLayout.layoutTransition = originalTransition
            }
        }

        dateRangePicker.show(parentFragmentManager, "DATE_PICKER")
    }
    @SuppressLint("UseCompatLoadingForDrawables")
    private fun setupWeeklyCalendar() {
        val calendar = Calendar.getInstance()
        val todayDateFormat = SimpleDateFormat("yyyy년 MM월 dd일 E요일", Locale.KOREAN)
        binding.topBarLayout.tvToday.text = todayDateFormat.format(calendar.time)
    }


    private fun setupRecyclerView() {
        journalAdapter = JournalAdapter()
        val linearLayoutManager = LinearLayoutManager(requireContext())
        binding.rvDiaryFeed.apply {
            layoutManager = linearLayoutManager
            adapter = journalAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        val lastVisibleItemPosition = linearLayoutManager.findLastVisibleItemPosition()
                        val totalItemCount = linearLayoutManager.itemCount
                        if (viewModel.isLoading.value == false && !viewModel.isLastPage) {
                            if (lastVisibleItemPosition >= totalItemCount - 3) {
                                viewModel.loadMoreJournals()
                            }
                        }
                    }
                }
            })
        }
    }

    private fun observeViewModel() {
        viewModel.journals.observe(viewLifecycleOwner, Observer { journalList ->
            journalAdapter.submitList(journalList.toList())
        })
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // 로딩 처리
        }
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            if (message != null) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

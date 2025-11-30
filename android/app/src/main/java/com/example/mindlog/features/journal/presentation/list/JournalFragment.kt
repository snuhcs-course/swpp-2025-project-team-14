package com.example.mindlog.features.journal.presentation.list

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.util.Pair
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mindlog.R
import com.example.mindlog.databinding.FragmentJournalListBinding
import com.example.mindlog.features.home.presentation.HomeActivity
import com.example.mindlog.features.journal.presentation.adapter.JournalAdapter
import com.example.mindlog.features.journal.presentation.detail.JournalDetailActivity
import com.example.mindlog.features.journal.presentation.write.JournalWriteActivity
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class JournalFragment : Fragment(), HomeActivity.FabClickListener {

    private var _binding: FragmentJournalListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: JournalViewModel by viewModels()
    private lateinit var journalAdapter: JournalAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    private var scrollToTopOnNextSubmit = false
    private var isEmpty = true
    private var isLoad = false

    override fun onFabClick() {
        val intent = Intent(requireContext(), JournalWriteActivity::class.java)
        activityResultLauncher.launch(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val updatedId = data?.getIntExtra(JournalDetailActivity.EXTRA_UPDATED_JOURNAL_ID, -1) ?: -1
                val deletedId = data?.getIntExtra(JournalDetailActivity.EXTRA_DELETED_JOURNAL_ID, -1) ?: -1

                when {
                    updatedId == -1 && deletedId == -1 -> {
                        scrollToTopOnNextSubmit = true
                        viewModel.clearSearchAndReload()
                    }
                    else -> {
                        viewModel.updateOrRemoveJournalEntry(
                            updatedId = if (updatedId == -1) null else updatedId,
                            deletedId = if (deletedId == -1) null else deletedId
                        )
                    }
                }
            }
        }
    }

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

        viewModel.startObservingSearchQuery()

        viewModel.loadJournals()
    }

    private fun setupClickListeners() {
        val topBar = binding.topBarLayout

        topBar.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.action_journalFragment_to_settingsFragment)
        }

        topBar.btnSearch.setOnClickListener {
            toggleSearchView(true)
        }

        topBar.btnSearchClose.setOnClickListener {
            toggleSearchView(false)
            if (viewModel.searchQuery.value?.isNotEmpty() == true) {
                viewModel.searchQuery.value = ""
                scrollToTopOnNextSubmit = true
                viewModel.clearSearchConditions()
                viewModel.loadJournals()
            }
        }

        topBar.btnDateRangeClose.setOnClickListener {
            val rootLayout = topBar.homeTopRoot
            val originalTransition = rootLayout.layoutTransition
            rootLayout.layoutTransition = null

            topBar.dateRangeBarContainer.visibility = View.GONE
            scrollToTopOnNextSubmit = true
            viewModel.setDateRange(null, null)
            viewModel.loadJournals()

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
                scrollToTopOnNextSubmit = true
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
            .setTheme(R.style.ThemeOverlay_MindLog_DatePicker)
            .setPositiveButtonText("검색")
            .build()

        dateRangePicker.addOnPositiveButtonClickListener { selection: Pair<Long, Long> ->
            val startDate = Date(selection.first)
            val endDate = Date(selection.second)

            val apiFormat = SimpleDateFormat("yyyy-MM-dd", Locale.KOREAN)
            val chipFormat = SimpleDateFormat("yyyy년 MM월 dd일 E요일", Locale.KOREAN)

            scrollToTopOnNextSubmit = true
            viewModel.setDateRange(apiFormat.format(startDate), apiFormat.format(endDate))
            viewModel.loadJournals()

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
        journalAdapter = JournalAdapter { journalId ->
            val intent = Intent(requireContext(), JournalDetailActivity::class.java).apply {
                putExtra(JournalDetailActivity.EXTRA_JOURNAL_ID, journalId)
            }
            activityResultLauncher.launch(intent)
        }
        linearLayoutManager = LinearLayoutManager(requireContext())
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
            isEmpty = journalList.isNullOrEmpty()
            isLoad = viewModel.isLoading.value == true

            binding.rvDiaryFeed.visibility = if (!isLoad && isEmpty) View.GONE else View.VISIBLE
            binding.emptyView.visibility = if (!isLoad && isEmpty) View.VISIBLE else View.GONE

            journalAdapter.submitList(journalList.toList()) {
                if (scrollToTopOnNextSubmit) {
                    binding.rvDiaryFeed.post {
                        linearLayoutManager.scrollToPositionWithOffset(0, 0)
                    }
                    scrollToTopOnNextSubmit = false
                }
            }
        })
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            isLoad = isLoading

            binding.rvDiaryFeed.visibility = if (!isLoad && isEmpty) View.GONE else View.VISIBLE
            binding.emptyView.visibility = if (!isLoad && isEmpty) View.VISIBLE else View.GONE
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

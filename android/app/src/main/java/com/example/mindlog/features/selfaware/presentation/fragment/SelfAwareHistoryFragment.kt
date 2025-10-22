package com.example.mindlog.features.selfaware.presentation.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import com.example.mindlog.R
import com.example.mindlog.databinding.FragmentSelfAwareHistoryBinding
import com.example.mindlog.features.selfaware.presentation.adapter.SelfAwareHistoryAdapter
import com.example.mindlog.features.selfaware.presentation.viewmodel.SelfAwareHistoryViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

@AndroidEntryPoint
class SelfAwareHistoryFragment : Fragment(R.layout.fragment_self_aware_history) {

    private var _binding: FragmentSelfAwareHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SelfAwareHistoryViewModel by viewModels()
    private val adapter by lazy { SelfAwareHistoryAdapter() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentSelfAwareHistoryBinding.bind(view)

        // Toolbar: 뒤로가기
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
            // 또는 findNavController().navigateUp()
        }

        // RecyclerView
        binding.recyclerAnswers.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerAnswers.adapter = adapter

        // 무한 스크롤(바닥 도달 시 다음 페이지 로드)
        binding.recyclerAnswers.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0) return
                val lm = rv.layoutManager as? LinearLayoutManager ?: return
                val lastVisible = lm.findLastVisibleItemPosition()
                val total = lm.itemCount
                // 끝에서 3개 남았을 때 미리 로드
                if (lastVisible >= total - 3) viewModel.loadNext()
            }
        })

        // 초기 로드
        viewModel.refresh()

        // 상태 구독
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { s ->
                    // 로딩 처리(필요하면 ProgressBar 등으로 바꿔도 됨)
                    // binding.progressBar.isVisible = s.isLoading

                    adapter.submitList(s.items) // DiffUtil이라 깜빡임 최소화
                    // empty view 표시 등
                    binding.recyclerAnswers.isEnabled = s.items.isNotEmpty()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
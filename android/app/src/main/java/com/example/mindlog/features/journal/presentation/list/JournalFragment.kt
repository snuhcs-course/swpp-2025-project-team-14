package com.example.mindlog.features.journal.presentation.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mindlog.databinding.FragmentJournalListBinding
import com.example.mindlog.features.journal.presentation.adapter.JournalAdapter
import dagger.hilt.android.AndroidEntryPoint

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

        setupRecyclerView()
        observeViewModel()

        // 첫 페이지 로드 시작 (loadJournals는 이제 새로고침/초기 로드용)
        viewModel.loadJournals()
    }

    override fun onResume() {
        super.onResume()
        // 여기서 데이터를 로드하면, 일기 작성 후 돌아왔을 때 목록이 새로고침됩니다.
        viewModel.loadJournals()
    }

    private fun setupRecyclerView() {
        journalAdapter = JournalAdapter()
        val linearLayoutManager = LinearLayoutManager(requireContext())
        binding.rvDiaryFeed.apply {
            layoutManager = linearLayoutManager
            adapter = journalAdapter

            // --- 무한 스크롤 리스너 추가 ---
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    // 스크롤이 아래로 내려갈 때만 확인
                    if (dy > 0) {
                        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                        val visibleItemCount = layoutManager.childCount
                        val totalItemCount = layoutManager.itemCount
                        val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                        // 로딩 중이 아닐 때, 마지막 아이템이 보이면 다음 페이지 로드
                        // (마지막 아이템 전에 미리 로드하려면 5 정도의 버퍼를 둔다)
                        if (viewModel.isLoading.value == false) {
                            if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5 && firstVisibleItemPosition >= 0) {
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
            journalAdapter.submitList(journalList)
        })

        // TODO: 로딩 인디케이터(ProgressBar) 표시/숨김 처리
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // TODO: 에러 메시지 토스트 등으로 표시
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            // message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

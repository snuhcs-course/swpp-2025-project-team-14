package com.example.mindlog.features.selfaware.presentation.fragment

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.mindlog.R
import com.example.mindlog.databinding.FragmentSelfAwareBinding
import com.example.mindlog.features.selfaware.presentation.viewmodel.SelfAwareViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SelfAwareFragment : Fragment(R.layout.fragment_self_aware) {
    private var _binding: FragmentSelfAwareBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SelfAwareViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentSelfAwareBinding.bind(view)

        // initial load
        viewModel.load()

        binding.etAnswer.doAfterTextChanged {
            viewModel.updateAnswerText(it?.toString().orEmpty())
        }

        binding.btnSubmit.setOnClickListener { viewModel.submit() }

        binding.btnOpenHistory.setOnClickListener {
            findNavController().navigate(R.id.selfAwareHistoryFragment)
        }

        // observe state
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { s ->
                    binding.btnSubmit.isEnabled = !s.isLoading && !s.isSubmitting

                    // 질문 입력/완료 토글
                    binding.groupQuestion.isVisible = !s.isAnsweredToday
                    binding.tvQuestion.text = s.questionText ?: "질문을 불러오는 중…"
                    binding.btnSubmit.isEnabled = s.questionText != null && s.answerText.isNotBlank()

                    // 가치 점수 UI 갱신 (예: 막대/레이더 등)
                    // binding.containerValues.removeAllViews() ...
                }
            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
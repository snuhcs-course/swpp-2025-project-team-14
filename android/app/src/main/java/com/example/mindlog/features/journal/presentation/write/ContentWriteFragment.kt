package com.example.mindlog.features.journal.presentation.write

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.mindlog.databinding.FragmentContentWriteBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class ContentWriteFragment : Fragment() {

    private var _binding: FragmentContentWriteBinding? = null
    private val binding get() = _binding!!

    // 1. Activity와 ViewModel 공유
    private val viewModel: JournalWriteViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContentWriteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setCurrentDate()
        setupTextWatchers()
    }

    private fun setCurrentDate() {
        val currentDate = Date()
        val format = SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault())
        binding.tvDate.text = format.format(currentDate)
    }

    // 2. EditText의 텍스트 변경을 감지하는 리스너 설정
    private fun setupTextWatchers() {
        binding.etTitle.addTextChangedListener { text ->
            // 텍스트가 변경될 때마다 ViewModel의 title StateFlow를 업데이트
            viewModel.title.value = text.toString()
        }

        binding.etContent.addTextChangedListener { text ->
            // 텍스트가 변경될 때마다 ViewModel의 content StateFlow를 업데이트
            viewModel.content.value = text.toString()
        }
        binding.etGratitude.addTextChangedListener { text ->
            viewModel.gratitude.value = text.toString()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

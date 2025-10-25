package com.example.mindlog.features.journal.presentation.write

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.mindlog.databinding.FragmentContentWriteBinding

class ContentWriteFragment : Fragment() {

    private var _binding: FragmentContentWriteBinding? = null
    private val binding get() = _binding!!

    // Hilt가 Activity 종류에 따라 올바른 ViewModel을 주입
    private val writeViewModel: JournalWriteViewModel by activityViewModels()
    private val editViewModel: JournalEditViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentContentWriteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (requireActivity() is JournalEditActivity) {
            setupForEdit()
        } else {
            setupForWrite()
        }
    }

    private fun setupForEdit() {
        // ViewModel의 LiveData를 관찰하여 EditText의 텍스트를 설정
        editViewModel.title.observe(viewLifecycleOwner) { if (binding.etTitle.text.toString() != it) binding.etTitle.setText(it) }
        editViewModel.content.observe(viewLifecycleOwner) { if (binding.etContent.text.toString() != it) binding.etContent.setText(it) }
        editViewModel.gratitude.observe(viewLifecycleOwner) { if (binding.etGratitude.text.toString() != it) binding.etGratitude.setText(it) }

        // EditText의 텍스트가 변경될 때마다 ViewModel의 LiveData를 업데이트
        binding.etTitle.doAfterTextChanged { text -> editViewModel.title.value = text.toString() }
        binding.etContent.doAfterTextChanged { text -> editViewModel.content.value = text.toString() }
        binding.etGratitude.doAfterTextChanged { text -> editViewModel.gratitude.value = text.toString() }
    }

    private fun setupForWrite() {
        // 기존 작성 로직과 동일
        binding.etTitle.doAfterTextChanged { text -> writeViewModel.title.value = text.toString() }
        binding.etContent.doAfterTextChanged { text -> writeViewModel.content.value = text.toString() }
        binding.etGratitude.doAfterTextChanged { text -> writeViewModel.gratitude.value = text.toString() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

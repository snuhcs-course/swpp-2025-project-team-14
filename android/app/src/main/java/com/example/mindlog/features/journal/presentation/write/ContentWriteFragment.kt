package com.example.mindlog.features.journal.presentation.write

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.mindlog.databinding.FragmentContentWriteBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ContentWriteFragment : Fragment() {

    private var _binding: FragmentContentWriteBinding? = null
    private val binding get() = _binding!!

    private val writeViewModel: JournalWriteViewModel by activityViewModels()
    private val editViewModel: JournalEditViewModel by activityViewModels()

    // ✨ [핵심 1] 갤러리 앱을 실행하고 이미지 Uri를 받아오는 Launcher 정의
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            if (requireActivity() is JournalWriteActivity) {
                writeViewModel.selectedImageUri.value = it
            }
            // TODO: 수정 모드일 때의 로직
        }
    }

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
        // ... 기존 수정 관련 로직은 그대로 둡니다 ...
        editViewModel.title.observe(viewLifecycleOwner) { if (binding.etTitle.text.toString() != it) binding.etTitle.setText(it) }
        editViewModel.content.observe(viewLifecycleOwner) { if (binding.etContent.text.toString() != it) binding.etContent.setText(it) }
        editViewModel.gratitude.observe(viewLifecycleOwner) { if (binding.etGratitude.text.toString() != it) binding.etGratitude.setText(it) }

        binding.etTitle.doAfterTextChanged { text -> editViewModel.title.value = text.toString() }
        binding.etContent.doAfterTextChanged { text -> editViewModel.content.value = text.toString() }
        binding.etGratitude.doAfterTextChanged { text -> editViewModel.gratitude.value = text.toString() }
    }

    private fun setupForWrite() {
        // --- 기존 텍스트 바인딩 로직 ---
        binding.etTitle.doAfterTextChanged { text -> writeViewModel.title.value = text.toString() }
        binding.etContent.doAfterTextChanged { text -> writeViewModel.content.value = text.toString() }
        binding.etGratitude.doAfterTextChanged { text -> writeViewModel.gratitude.value = text.toString() }

        // ✨ [핵심 2] XML에 있는 ID를 사용하여 클릭 리스너를 설정합니다.
        // 1. 이미지 영역 전체 클릭 리스너
        binding.layoutAddImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
        // 2. 하단 '사진 추가' 버튼 클릭 리스너
        binding.btnAddPhoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }


        // ✨ [핵심 3] 이미지 Uri 변경을 감지하고 UI에 정확하게 반영합니다.
        viewLifecycleOwner.lifecycleScope.launch {
            writeViewModel.selectedImageUri.collectLatest { uri ->
                if (uri != null) {
                    // 이미지가 선택된 경우
                    binding.ivPreview.visibility = View.VISIBLE
                    binding.ivPreview.setImageURI(uri)
                    binding.tvAddImagePlaceholder.visibility = View.GONE // "사진을 추가해보세요" 텍스트 숨김
                    binding.btnAddPhoto.text = "사진 변경" // 하단 버튼 텍스트 변경
                } else {
                    // 이미지가 없는 경우 (초기 상태 또는 삭제 시)
                    binding.ivPreview.visibility = View.GONE
                    binding.tvAddImagePlaceholder.visibility = View.VISIBLE // "사진을 추가해보세요" 텍스트 보임
                    binding.btnAddPhoto.text = "사진 추가" // 하단 버튼 텍스트 복원
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

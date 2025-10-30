package com.example.mindlog.features.journal.presentation.write

import android.graphics.Bitmap // ✨ [추가]
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar // ✨ [추가]
import android.widget.Toast // ✨ [추가]
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog // ✨ [추가]
import androidx.core.view.isVisible // ✨ [추가]
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.mindlog.databinding.FragmentContentWriteBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ContentWriteFragment : Fragment() {

    private var _binding: FragmentContentWriteBinding? = null
    private val binding get() = _binding!!

    // '작성'과 '수정' ViewModel을 모두 주입받음
    private val writeViewModel: JournalWriteViewModel by activityViewModels()
    private val editViewModel: JournalEditViewModel by activityViewModels()

    // 현재 컨텍스트(작성/수정)에 맞는 ViewModel과 이미지 Uri StateFlow를 가져오는 getter
    private val currentViewModel: ViewModel
        get() = if (activity is JournalEditActivity) editViewModel else writeViewModel
    private val currentImageUriState: MutableStateFlow<Uri?>
        get() = if (activity is JournalEditActivity) editViewModel.selectedImageUri else writeViewModel.selectedImageUri

    private val getPicture = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            currentImageUriState.value = it
            // AI 생성 이미지가 있었다면 초기화
            if (currentViewModel is JournalWriteViewModel) {
                (currentViewModel as JournalWriteViewModel).generatedImageBitmap.value = null
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContentWriteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (activity is JournalEditActivity) {
            bindForEdit()
            observeEditViewModel()
        } else {
            bindForWrite()
            observeWriteViewModel()
        }

        setupClickListeners()
    }

    // '작성' 모드 바인딩
    private fun bindForWrite() {
        binding.etTitle.setText(writeViewModel.title.value)
        binding.etContent.setText(writeViewModel.content.value)
        binding.etGratitude.setText(writeViewModel.gratitude.value)

        binding.etTitle.addTextChangedListener { writeViewModel.title.value = it.toString() }
        binding.etContent.addTextChangedListener { writeViewModel.content.value = it.toString() }
        binding.etGratitude.addTextChangedListener { writeViewModel.gratitude.value = it.toString() }
    }

    // '수정' 모드 바인딩
    private fun bindForEdit() {
        editViewModel.title.observe(viewLifecycleOwner) { if (binding.etTitle.text.toString() != it) binding.etTitle.setText(it) }
        editViewModel.content.observe(viewLifecycleOwner) { if (binding.etContent.text.toString() != it) binding.etContent.setText(it) }
        editViewModel.gratitude.observe(viewLifecycleOwner) { if (binding.etGratitude.text.toString() != it) binding.etGratitude.setText(it) }

        binding.etTitle.addTextChangedListener { editViewModel.title.value = it.toString() }
        binding.etContent.addTextChangedListener { editViewModel.content.value = it.toString() }
        binding.etGratitude.addTextChangedListener { editViewModel.gratitude.value = it.toString() }
    }

    // '작성' ViewModel 관찰
    private fun observeWriteViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            writeViewModel.selectedImageUri.collectLatest { uri ->
                updateImageView(uri, null, null)
            }
        }
        // ✨ [추가] AI 이미지 생성 관련 상태 관찰
        viewLifecycleOwner.lifecycleScope.launch {
            writeViewModel.isLoading.collect { isLoading ->
                binding.loadingOverlay.isVisible = isLoading            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            writeViewModel.generatedImageBitmap.collect { bitmap ->
                if(bitmap != null) updateImageView(null, null, bitmap)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            writeViewModel.aiGenerationError.collect { error ->
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            }
        }
    }

    // '수정' ViewModel 관찰
    private fun observeEditViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            editViewModel.selectedImageUri.collectLatest { newUri ->
                if (newUri != null) {
                    updateImageView(newUri, null, null)
                } else {
                    updateImageView(null, editViewModel.existingImageUrl.value, null)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            editViewModel.existingImageUrl.collectLatest { existingUrl ->
                if (editViewModel.selectedImageUri.value == null) {
                    updateImageView(null, existingUrl, null)
                }
            }
        }
        // TODO: 수정 화면에서도 AI 이미지 생성을 지원하려면 여기에 로직 추가
    }

    private fun setupClickListeners() {
        binding.layoutAddImage.setOnClickListener {
            getPicture.launch("image/*")
        }

        // ✨ [추가] AI 이미지 생성 버튼 리스너
        binding.btnGenerateAiPhoto.setOnClickListener {
            if(currentViewModel is JournalWriteViewModel) {
                showAiStyleDialog()
            } else {
                Toast.makeText(context, "수정 화면에서는 아직 지원되지 않는 기능입니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ✨ [추가] AI 스타일 선택 다이얼로그 표시 함수
    private fun showAiStyleDialog() {
        val styles = arrayOf("natural", "american-comics", "watercolor", "3d-animation", "pixel-art")
        AlertDialog.Builder(requireContext())
            .setTitle("AI 이미지 스타일 선택")
            .setItems(styles) { _, which ->
                val selectedStyle = styles[which]
                writeViewModel.generateAiImage(selectedStyle)
            }
            .setNegativeButton("취소", null)
            .show()
    }


    // ✨ [수정] Uri, Url, Bitmap을 모두 처리하도록 함수 시그니처 변경
    private fun updateImageView(uri: Uri?, url: String?, bitmap: Bitmap?) {
        val imageSource: Any? = uri ?: url ?: bitmap

        if (imageSource != null) {
            binding.ivPreview.visibility = View.VISIBLE
            binding.tvAddImagePlaceholder.visibility = View.GONE
            Glide.with(this)
                .load(imageSource)
                .into(binding.ivPreview)
        } else {
            binding.ivPreview.visibility = View.GONE
            binding.tvAddImagePlaceholder.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

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
import androidx.compose.ui.semantics.dismiss
import androidx.core.view.isVisible // ✨ [추가]
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.mindlog.databinding.DialogAiStyleSelectorBinding
import com.example.mindlog.databinding.FragmentContentWriteBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.widget.RadioButton

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
            when (val vm = currentViewModel) {
                is JournalWriteViewModel -> vm.setGalleryImageUri(it)
                is JournalEditViewModel -> vm.setGalleryImageUri(it)
            }
            updateImageView(it, null, null)
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

    private fun observeEditViewModel() {
        // ✨ [핵심 3] 수정 모드의 관찰 로직을 작성 모드와 동일하게 단순화합니다.
        // 이렇게 하면 각 Flow가 독립적으로 UI를 업데이트하여 상태 충돌을 방지합니다.

        // 1. 갤러리 이미지 관찰
        viewLifecycleOwner.lifecycleScope.launch {
            editViewModel.selectedImageUri.collectLatest { uri ->
                if (uri != null) updateImageView(uri, null, null)
            }
        }
        // 2. 기존 서버 이미지 관찰
        viewLifecycleOwner.lifecycleScope.launch {
            editViewModel.existingImageUrl.collectLatest { url ->
                // 갤러리나 AI 이미지가 없을 때만 기존 이미지를 보여줍니다.
                if (editViewModel.selectedImageUri.value == null && editViewModel.generatedImageBitmap.value == null) {
                    updateImageView(null, url, null)
                }
            }
        }
        // 3. AI 생성 이미지 관찰
        viewLifecycleOwner.lifecycleScope.launch {
            editViewModel.generatedImageBitmap.collect { bitmap ->
                if (bitmap != null) updateImageView(null, null, bitmap)
            }
        }

        // --- 로딩 및 에러 관찰 (기존과 동일) ---
        viewLifecycleOwner.lifecycleScope.launch {
            editViewModel.isLoading.collect { isLoading ->
                binding.loadingOverlay.isVisible = isLoading
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            editViewModel.aiGenerationError.collect { error ->
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            }
        }
    }


    private fun setupClickListeners() {
        // ✨ [핵심 수정] 하단의 '사진 추가' 버튼 클릭 리스너
        binding.btnAddPhoto.setOnClickListener {
            getPicture.launch("image/*")
        }

        // 이미지 미리보기 영역(상단) 클릭 리스너
        binding.ivPreview.setOnClickListener {
            getPicture.launch("image/*")
        }

        // 이미지 추가 플레이스홀더(상단, 이미지 없을 때) 클릭 리스너
        binding.layoutAddImage.setOnClickListener {
            getPicture.launch("image/*")
        }

        // 'AI 사진 생성' 버튼 클릭 리스너
        binding.btnGenerateAiPhoto.setOnClickListener {
            showAiStyleDialog()
        }
    }


    private fun showAiStyleDialog() {
        // 1. Inflater를 사용하여 커스텀 레이아웃을 View 객체로 변환하고, 데이터 바인딩을 활성화합니다.
        val dialogBinding = DialogAiStyleSelectorBinding.inflate(LayoutInflater.from(requireContext()))

        // 2. MaterialAlertDialogBuilder를 사용하여 다이얼로그를 생성합니다.
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root) // 우리가 만든 커스텀 뷰를 설정합니다.
            .setCancelable(true) // 다이얼로그 바깥 영역을 터치하면 닫히도록 설정합니다.
            .show()

        // 3. '취소' 버튼 클릭 리스너를 설정합니다.
        dialogBinding.btnDialogCancel.setOnClickListener {
            dialog.dismiss() // 다이얼로그를 닫습니다.
        }

        // 4. '생성' 버튼 클릭 리스너를 설정합니다.
        dialogBinding.btnDialogGenerate.setOnClickListener {
            // 현재 선택된 RadioButton의 ID를 찾습니다.
            val selectedRadioButtonId = dialogBinding.rgStyleOptions.checkedRadioButtonId

            // ✨ [핵심 수정] 타입을 android.widget.RadioButton으로 정확하게 지정합니다.
            val selectedRadioButton = dialogBinding.root.findViewById<android.widget.RadioButton>(selectedRadioButtonId)

            // ✨ [추가] 아무것도 선택되지 않았을 경우의 예외 처리
            if (selectedRadioButtonId == View.NO_ID) { // NO_ID는 -1 입니다.
                Toast.makeText(requireContext(), "스타일을 선택해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener // 함수 실행을 여기서 중단합니다.
            }

            // RadioButton의 tag 속성에 저장해 둔 스타일 문자열(예: "natural")을 가져옵니다.
            val selectedStyle = selectedRadioButton.tag.toString()

            // ✨ [핵심 수정] ViewModel 종류에 따라 맞는 generateImage 함수 호출
            when (val vm = currentViewModel) {
                is JournalWriteViewModel -> vm.generateImage(selectedStyle)
                is JournalEditViewModel -> vm.generateImage(selectedStyle)
            }

            dialog.dismiss() // 다이얼로그를 닫습니다.
        }
    }


    private fun updateImageView(uri: Uri?, url: String?, bitmap: Bitmap?) {
        val imageSource: Any? = uri ?: url ?: bitmap

        // ✨ [핵심 수정] 이미지 소스가 있으면(null이 아니면) 관련 뷰들을 모두 숨김
        if (imageSource != null) {
            binding.ivPreview.visibility = View.GONE
            binding.tvAddImagePlaceholder.visibility = View.GONE
            // Glide로 로드하는 코드는 남겨두지만, 뷰가 숨겨져 있으므로 화면에는 보이지 않습니다.
            Glide.with(this)
                .load(imageSource)
                .into(binding.ivPreview)
        } else {
            // 이미지가 없으면 플레이스홀더를 다시 보이게 함
            binding.ivPreview.visibility = View.GONE
            binding.tvAddImagePlaceholder.visibility = View.VISIBLE
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

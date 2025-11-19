package com.example.mindlog.features.settings.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mindlog.R
import com.example.mindlog.databinding.FragmentChangeIdBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChangeIdFragment : Fragment() {

    private var _binding: FragmentChangeIdBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChangeIdBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 뒤로가기 버튼
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        // '저장' 버튼 클릭 시 확인 다이얼로그 띄우기
        binding.btnSave.setOnClickListener {
            // TODO: 새 아이디 입력값에 대한 유효성 검사 로직 추가 (e.g., 비어있는지, 중복되는지 등)
            showConfirmDialog()
        }
    }


    private fun showConfirmDialog() {
        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_MindLog_AlertDialog)
        .setTitle("아이디 변경")
            .setMessage("정말로 아이디를 변경하시겠습니까?")
            .setNegativeButton("취소", null)
            .setPositiveButton("확인") { _, _ ->
                // TODO: ViewModel을 통해 아이디 변경 API 호출
                findNavController().navigateUp()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

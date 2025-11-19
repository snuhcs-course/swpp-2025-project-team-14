package com.example.mindlog.features.settings.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mindlog.R // R 클래스 import
import com.example.mindlog.databinding.FragmentChangePasswordBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder // Builder import
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChangePasswordFragment : Fragment() {

    private var _binding: FragmentChangePasswordBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChangePasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 뒤로가기 버튼
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        // '저장' 버튼 클릭 시 확인 다이얼로그를 띄우도록 수정
        binding.btnSave.setOnClickListener {
            // TODO: 현재/새 비밀번호 입력값에 대한 유효성 검사 로직 추가
            showConfirmDialog()
        }
    }

    private fun showConfirmDialog() {
        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_MindLog_AlertDialog)
            .setTitle("비밀번호 변경")
            .setMessage("정말로 비밀번호를 변경하시겠습니까?")
            .setNegativeButton("취소", null)
            .setPositiveButton("확인") { _, _ ->
                // TODO: ViewModel을 통해 비밀번호 변경 API 호출
                findNavController().navigateUp()
            }
            .show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.example.mindlog.features.settings.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.mindlog.R
import com.example.mindlog.core.common.Result
import com.example.mindlog.databinding.FragmentChangePasswordBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.mindlog.features.home.presentation.HomeActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChangePasswordFragment : Fragment(), HomeActivity.FabClickListener {

    private var _binding: FragmentChangePasswordBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChangePasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSave.setOnClickListener {
            val currentPw = binding.etCurrentPassword.text.toString()
            val newPw = binding.etNewPassword.text.toString()
            val confirmPw = binding.etConfirmPassword.text.toString()

            if (currentPw.isBlank()) {
                binding.tilCurrentPassword.error = "현재 비밀번호를 입력해주세요."
                return@setOnClickListener
            } else {
                binding.tilCurrentPassword.error = null
            }

            if (newPw.isBlank()) {
                binding.tilNewPassword.error = "새 비밀번호를 입력해주세요."
                return@setOnClickListener
            }
            if (newPw != confirmPw) {
                binding.tilConfirmPassword.error = "비밀번호가 일치하지 않습니다."
                return@setOnClickListener
            }

            binding.tilNewPassword.error = null
            binding.tilConfirmPassword.error = null

            showConfirmDialog(currentPw, newPw)
        }

        observeViewModel()
    }

    override fun onFabClick() {
        Toast.makeText(requireContext(), "먼저 설정을 완료해주세요.", Toast.LENGTH_SHORT).show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.updateResult.collect { result ->
                    when (result) {
                        is Result.Success -> {
                            Toast.makeText(requireContext(), result.data, Toast.LENGTH_SHORT).show()
                            findNavController().navigateUp()
                        }
                        is Result.Error -> {
                            val friendlyMessage = getUserFriendlyErrorMessage(result.message)
                            Toast.makeText(requireContext(), getUserFriendlyErrorMessage(result.message), Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun getUserFriendlyErrorMessage(originalMessage: String?): String {
        if (originalMessage == null) return "알 수 없는 오류가 발생했습니다. 잠시 후 다시 시도해주세요."

        return when {
            originalMessage.contains("current", ignoreCase = true) ->
                "현재 비밀번호가 일치하지 않습니다. 다시 확인해주세요."

            originalMessage.contains("format", ignoreCase = true) ->
                "8~20자, 영문/숫자/특수문자 중 2가지 이상을 조합해주세요."

            else -> "비밀번호 변경에 실패했습니다. "
        }
    }

    private fun showConfirmDialog(currentPw: String, newPw: String) {
        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_MindLog_AlertDialog)
            .setTitle("비밀번호 변경")
            .setMessage("정말로 비밀번호를 변경하시겠습니까?")
            .setNegativeButton("취소", null)
            .setPositiveButton("확인") { _, _ ->
                viewModel.updatePassword(currentPw, newPw)
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

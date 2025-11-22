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
import com.example.mindlog.core.common.Result
import com.example.mindlog.databinding.FragmentEditProfileBinding
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@AndroidEntryPoint
class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupDatePicker()
        setupSaveButton()

        // 데이터 관찰 및 로드
        observeViewModel()
        viewModel.loadUserInfo()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupDatePicker() {
        binding.etBirthdate.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("생년월일 선택")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                sdf.timeZone = TimeZone.getTimeZone("UTC") // 선택된 값은 UTC 기준
                val dateString = sdf.format(Date(selection))
                binding.etBirthdate.setText(dateString)
            }
            datePicker.show(parentFragmentManager, "BIRTHDATE_PICKER")
        }
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()

            if (username.isBlank()) {
                binding.tilUsername.error = "이름을 입력해주세요."
                return@setOnClickListener
            }
            binding.tilUsername.error = null

            val gender = when (binding.rgGender.checkedRadioButtonId) {
                binding.rbMale.id -> "M"
                binding.rbFemale.id -> "F"
                binding.rbOther.id -> "O"
                else -> null
            }

            val birthdate = binding.etBirthdate.text.toString().trim().ifBlank { null }
            val appearance = binding.etAppearance.text.toString().trim().ifBlank { null }

            viewModel.updateProfile(username, gender, birthdate, appearance)
        }
    }

    private fun observeViewModel() {
        // 1. 초기 데이터 로드 시 UI 채우기
        viewModel.userInfo.observe(viewLifecycleOwner) { result ->
            if (result is Result.Success) {
                val user = result.data
                // 사용자가 아직 수정하지 않은 경우에만 데이터를 채움 (혹은 로딩 완료 시점 체크)
                if (binding.etUsername.text.isNullOrBlank()) {
                    binding.etUsername.setText(user.username)
                    binding.etBirthdate.setText(user.birthdate ?: "")
                    binding.etAppearance.setText(user.appearance ?: "")

                    when (user.gender) {
                        "M" -> binding.rbMale.isChecked = true
                        "F" -> binding.rbFemale.isChecked = true
                        "O" -> binding.rbOther.isChecked = true
                    }
                }
            } else if (result is Result.Error) {
                Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
            }
        }

        // 2. 저장 결과 처리
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.updateResult.collect { result ->
                    when (result) {
                        is Result.Success -> {
                            Toast.makeText(requireContext(), result.data, Toast.LENGTH_SHORT).show()
                            findNavController().navigateUp()
                        }
                        is Result.Error -> {
                            Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

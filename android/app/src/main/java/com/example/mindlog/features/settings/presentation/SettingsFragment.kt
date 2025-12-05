package com.example.mindlog.features.settings.presentation

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.mindlog.R
import com.example.mindlog.core.domain.Result
import com.example.mindlog.databinding.FragmentSettingsBinding
import com.example.mindlog.features.auth.presentation.login.LoginActivity
import com.example.mindlog.features.home.presentation.HomeActivity
import com.example.mindlog.features.journal.presentation.write.JournalWriteActivity
import com.example.mindlog.features.tutorial.TutorialActivity
import com.example.mindlog.features.tutorial.TutorialMenuActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment(), HomeActivity.FabClickListener {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupFab()
        setupToolbar()
        setupClickListeners()
        observeViewModel()

        viewModel.loadUserInfo()
    }

    private fun setupFab() {
        // Journal 작성 화면에서 돌아올 때 결과를 처리하기 위한 launcher 설정
        activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // 작성 완료 시 홈의 Journal 탭으로 이동
                (activity as? HomeActivity)?.let { homeActivity ->
                    findNavController().navigateUp()
                    homeActivity.navigateToJournalTab()
                }
            }
        }
    }

    override fun onFabClick() {
        val intent = Intent(requireContext(), JournalWriteActivity::class.java)
        activityResultLauncher.launch(intent)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupClickListeners() {
        binding.btnChangePassword.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_changePasswordFragment)
        }

        binding.btnEditProfile.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_editProfileFragment)
        }

        binding.btnTutorial.setOnClickListener {
            val intent = Intent(requireContext(), TutorialMenuActivity::class.java).apply {
                putExtra(TutorialActivity.EXTRA_RETURN_TO_SETTINGS, true)
            }
            startActivity(intent)
        }

        binding.btnLogout.setOnClickListener {
            showLogoutConfirmDialog()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.logoutEvent.collect { result ->
                    when (result) {
                        is Result.Success -> {
                            val intent = Intent(requireActivity(), LoginActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(intent)
                            requireActivity().finish()
                        }
                        is Result.Error -> {
                            Log.e("SettingsFragment", "로그아웃 에러: ${result.message}")
                            Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        viewModel.userInfo.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Success -> {
                    val user = result.data
                    binding.tvUsername.text = user.username

                    binding.tvLoginId.text = "ID: ${user.loginId}"

                    val infoParts = mutableListOf<String>()
                    user.birthdate?.let { infoParts.add(it) }
                    user.gender?.let { gender ->
                        val displayGender = when (gender.uppercase()) {
                            "M", "MALE" -> "남자"
                            "F", "FEMALE" -> "여자"
                            else -> "기타"
                        }
                        infoParts.add(displayGender)
                    }

                    if (infoParts.isNotEmpty()) {
                        binding.tvAdditionalInfo.text = infoParts.joinToString(" · ")
                        binding.tvAdditionalInfo.isVisible = true
                    } else {
                        binding.tvAdditionalInfo.isVisible = false
                    }
                }
                is Result.Error -> {
                    Log.e("SettingsFragment", "유저 정보 로드 에러: ${result.message}")
                    Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showLogoutConfirmDialog() {
        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_MindLog_AlertDialog)
            .setTitle("로그아웃")
            .setMessage("정말로 로그아웃 하시겠습니까?")
            .setNegativeButton("취소", null)
            .setPositiveButton("로그아웃") { _, _ ->
                viewModel.logout()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

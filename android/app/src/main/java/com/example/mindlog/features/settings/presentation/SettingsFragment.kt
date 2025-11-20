package com.example.mindlog.features.settings.presentation

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.mindlog.databinding.FragmentSettingsBinding
import com.example.mindlog.features.auth.presentation.login.LoginActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment(), LogoutDialogFragment.ConfirmDialogListener {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    // 'by viewModels()'를 통해 SettingsViewModel을 가져옵니다.
    private val viewModel: SettingsViewModel by viewModels()

    // 다이얼로그에서 '로그아웃' 버튼을 누르면 이 함수가 호출됩니다.
    override fun onConfirm() {
        viewModel.logout()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp() // 뒤로가기
        }
    }

    private fun setupClickListeners() {
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmDialog()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.logoutEvent.collect { result ->
                result.onSuccess {
                    val intent = Intent(requireActivity(), LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    requireActivity().finish()
                }.onFailure {
                    Toast.makeText(requireContext(), "로그아웃에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showLogoutConfirmDialog() {
        val dialog = LogoutDialogFragment.newInstance()
        dialog.setConfirmDialogListener(this)
        dialog.show(parentFragmentManager, "LogoutDialogFragment")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

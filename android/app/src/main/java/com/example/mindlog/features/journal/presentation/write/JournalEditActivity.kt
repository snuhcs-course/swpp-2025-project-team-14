package com.example.mindlog.features.journal.presentation.write

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.mindlog.R
import com.example.mindlog.core.domain.Result
import com.example.mindlog.core.ui.SystemUiHelper
import com.example.mindlog.databinding.ActivityJournalEditBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class JournalEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityJournalEditBinding
    private val viewModel: JournalEditViewModel by viewModels()

    companion object {
        const val EXTRA_JOURNAL_ID = "EXTRA_JOURNAL_ID"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJournalEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SystemUiHelper.hideSystemUI(this)

        val journalId = intent.getIntExtra(EXTRA_JOURNAL_ID, -1)
        if (journalId == -1) {
            Toast.makeText(this, "잘못된 접근입니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupFragment()
        setupClickListeners()
        setupOnBackPressed()
        observeViewModel()

        viewModel.loadJournalDetails(journalId)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            checkImageGenerationBeforeAction {
                finish()
            }
        }
    }

    private fun setupFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.edit_fragment_container, ContentWriteFragment())
            .commit()
    }

    private fun setupClickListeners() {
        binding.btnEditSave.setOnClickListener {
            checkImageGenerationBeforeAction {
                viewModel.updateJournal()
            }
        }
    }

    private fun setupOnBackPressed() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                checkImageGenerationBeforeAction {
                    finish()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }

    private fun checkImageGenerationBeforeAction(action: () -> Unit) {
        if (viewModel.isLoading.value) {
            MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_MindLog_AlertDialog)
                .setTitle("이미지 생성 중")
                .setMessage("이미지를 생성하고 있어요.\n지금 이동하거나 저장하면 생성이 취소됩니다.\n계속하시겠습니까?")
                .setNegativeButton("취소", null)
                .setPositiveButton("확인") { _, _ ->
                    action()
                }
                .show()
        } else {
            action()
        }
    }

    private fun observeViewModel() {
        viewModel.journalState.observe(this) { result ->
            when (result) {
                is Result.Success -> {
                    binding.editFragmentContainer.visibility = View.VISIBLE
                }
                is Result.Error -> {
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.editResult.collect { result ->
                when (result) {
                    is Result.Success -> {
                        Toast.makeText(this@JournalEditActivity, result.data, Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    }
                    is Result.Error -> {
                        Toast.makeText(this@JournalEditActivity, result.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun showDeleteConfirmDialog() {
        MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_MindLog_AlertDialog)
            .setTitle("일기 삭제")
            .setMessage("정말로 이 일기를 삭제하시겠습니까? 삭제된 데이터는 복구할 수 없습니다.")
            .setNegativeButton("취소", null)
            .setPositiveButton("삭제") { _, _ ->
                viewModel.deleteJournal()
            }
            .show()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            SystemUiHelper.hideSystemUI(this)
        }
    }
}

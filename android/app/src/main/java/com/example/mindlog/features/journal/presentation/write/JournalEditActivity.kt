package com.example.mindlog.features.journal.presentation.write

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.mindlog.R
import com.example.mindlog.core.common.Result
import com.example.mindlog.core.common.SystemUiHelper
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
        observeViewModel()

        viewModel.loadJournalDetails(journalId)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupFragment() {
        // ContentWriteFragment를 재사용
        supportFragmentManager.beginTransaction()
            .replace(R.id.edit_fragment_container, ContentWriteFragment())
            .commit()
    }

    private fun setupClickListeners() {
        binding.btnEditSave.setOnClickListener {
            viewModel.updateJournal()
        }

        binding.btnEditDelete.setOnClickListener {
            showDeleteConfirmDialog()
        }
    }

    private fun observeViewModel() {
        // 데이터 로딩 상태 관찰
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

        // 수정/삭제 결과 관찰
        lifecycleScope.launch {
            viewModel.editResult.collect { result ->
                when (result) {
                    is Result.Success -> {
                        Toast.makeText(this@JournalEditActivity, result.data, Toast.LENGTH_SHORT).show()
                        setResult(Activity.RESULT_OK) // 피드 화면에 변경사항을 알림
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

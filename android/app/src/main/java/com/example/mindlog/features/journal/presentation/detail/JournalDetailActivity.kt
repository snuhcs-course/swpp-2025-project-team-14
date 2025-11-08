package com.example.mindlog.features.journal.presentation.detail

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.mindlog.R
import com.example.mindlog.core.common.Result
import com.example.mindlog.core.common.SystemUiHelper
import com.example.mindlog.databinding.ActivityJournalDetailBinding
import com.example.mindlog.features.journal.presentation.write.JournalEditActivity
import com.example.mindlog.features.journal.presentation.write.JournalEditViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class JournalDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityJournalDetailBinding
    private val viewModel: JournalEditViewModel by viewModels()

    companion object {
        const val EXTRA_JOURNAL_ID = "EXTRA_JOURNAL_ID"
    }

    // 수정 화면에서 돌아왔을 때 목록을 새로고침하기 위한 ActivityResultLauncher
    private val editJournalLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            setResult(Activity.RESULT_OK) // JournalFragment(목록)에 변경사항 알림
            viewModel.loadJournalDetails(viewModel.journalId!!, forceRefresh = true) // 현재 화면도 새로고침
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJournalDetailBinding.inflate(layoutInflater)
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
        supportFragmentManager.beginTransaction()
            .replace(R.id.detail_fragment_container, JournalDetailFragment())
            .commit()
    }

    private fun setupClickListeners() {
        // 수정 버튼 클릭
        binding.btnDetailEdit.setOnClickListener {
            val intent = Intent(this, JournalEditActivity::class.java).apply {
                putExtra(JournalEditActivity.EXTRA_JOURNAL_ID, viewModel.journalId)
            }
            editJournalLauncher.launch(intent)
        }

        // 삭제 버튼 클릭
        binding.btnDetailDelete.setOnClickListener {
            showDeleteConfirmDialog()
        }
    }

    private fun observeViewModel() {
        // 삭제 결과 관찰
        lifecycleScope.launch {
            viewModel.editResult.collect { result ->
                if (result is Result.Success && result.data == "삭제 완료") {
                    Toast.makeText(this@JournalDetailActivity, "삭제되었습니다.", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                } else if (result is Result.Error) {
                    Toast.makeText(this@JournalDetailActivity, result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showDeleteConfirmDialog() {
        MaterialAlertDialogBuilder(this)
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

package com.example.mindlog.features.journal.presentation.detail

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.mindlog.R
import com.example.mindlog.core.common.Result
import com.example.mindlog.core.common.SystemUiHelper
import com.example.mindlog.databinding.ActivityJournalDetailBinding
import com.example.mindlog.features.journal.presentation.write.ContentWriteFragment
import com.example.mindlog.features.journal.presentation.write.JournalEditActivity
import com.example.mindlog.features.journal.presentation.write.JournalEditViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class JournalDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityJournalDetailBinding
    private val viewModel: JournalEditViewModel by viewModels() // ViewModel은 수정 화면과 공유

    companion object {
        const val EXTRA_JOURNAL_ID = "EXTRA_JOURNAL_ID"
    }

    // 수정 화면에서 돌아왔을 때 목록을 새로고침하기 위한 런처
    private val editLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // 수정이 완료되었으므로, 상세 화면도 최신 데이터로 갱신
            viewModel.journalId?.let { id ->
                viewModel.loadJournalDetails(id, forceRefresh = true)
            }
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
            .replace(R.id.detail_fragment_container, ContentWriteFragment())
            .commit()
    }

    private fun setupClickListeners() {
        // 수정 아이콘 클릭 시, JournalEditActivity를 실행
        binding.btnEdit.setOnClickListener {
            val intent = Intent(this, JournalEditActivity::class.java).apply {
                putExtra(JournalEditActivity.EXTRA_JOURNAL_ID, viewModel.journalId)
            }
            editLauncher.launch(intent)
        }
    }

    private fun observeViewModel() {
        viewModel.journalState.observe(this) { result ->
            when (result) {
                is Result.Success -> {
                    binding.detailFragmentContainer.visibility = View.VISIBLE
                }
                is Result.Error -> {
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            SystemUiHelper.hideSystemUI(this)
        }
    }
}

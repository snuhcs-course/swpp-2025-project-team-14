package com.example.mindlog.features.journal.presentation.write

import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.semantics.setText
import androidx.lifecycle.lifecycleScope
import com.example.mindlog.R
import com.example.mindlog.core.common.Result
import com.example.mindlog.databinding.ActivityJournalWriteBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class JournalWriteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityJournalWriteBinding

    private val emotionSelectFragment by lazy { EmotionSelectFragment() }
    private val contentWriteFragment by lazy { ContentWriteFragment() }

    // 1. ViewModel을 주입받음
    private val viewModel: JournalWriteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJournalWriteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.write_fragment_container, emotionSelectFragment)
                .commit()
            updateButtonsForEmotionSelect()
        }

        setupButtonClickListeners()
        setupOnBackPressed()
        observeViewModel() // 3. ViewModel의 상태 변화를 관찰하는 함수 호출
    }

    private fun setupButtonClickListeners() {
        binding.btnCancelOrBack.setOnClickListener {
            handleBackButton()
        }

        binding.btnNextOrSave.setOnClickListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.write_fragment_container)
            if (currentFragment is EmotionSelectFragment) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.write_fragment_container, contentWriteFragment)
                    .addToBackStack(null)
                    .commit()
                updateButtonsForContentWrite()
            } else {
                // 2. '작성' 버튼 클릭 시 ViewModel의 saveJournal 함수 호출
                viewModel.saveJournal()
            }
        }
    }

    // 3. ViewModel의 saveResult 이벤트를 구독하고 결과에 따라 UI 처리

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.saveResult.collect { result ->
                when (result) {
                    is Result.Success -> {
                        Toast.makeText(this@JournalWriteActivity, "일기가 저장되었습니다.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    is Result.Error -> {
                        binding.btnNextOrSave.isEnabled = true
                        binding.btnCancelOrBack.isEnabled = true
                        val errorMessage = result.message ?: "알 수 없는 오류가 발생했습니다."

                        // ✨ [핵심 수정] 토스트 대신 '감사한 일' 칸에 에러 메시지 표시
                        // 현재 화면이 contentWriteFragment일 때만 실행
                        val currentFragment = supportFragmentManager.findFragmentById(R.id.write_fragment_container)
                        if (currentFragment is ContentWriteFragment) {
                            // 프래그먼트의 뷰에서 '감사한 일' EditText를 찾음
                            val gratitudeEditText = currentFragment.view?.findViewById<EditText>(R.id.et_gratitude)
                            gratitudeEditText?.setText(errorMessage)
                            Toast.makeText(this@JournalWriteActivity, "에러 발생! 상세 내용은 '감사한 일' 칸을 확인하세요.", Toast.LENGTH_LONG).show()
                        } else {
                            // 만약 다른 화면이라면 기존처럼 토스트 사용
                            Toast.makeText(this@JournalWriteActivity, errorMessage, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun handleBackButton() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
            updateButtonsForEmotionSelect()
        } else {
            finish()
        }
    }

    private fun setupOnBackPressed() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackButton()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }

    private fun updateButtonsForEmotionSelect() {
        binding.btnCancelOrBack.text = "취소"
        binding.btnNextOrSave.text = "다음"
    }

    private fun updateButtonsForContentWrite() {
        binding.btnCancelOrBack.text = "뒤로"
        binding.btnNextOrSave.text = "작성"
    }
}

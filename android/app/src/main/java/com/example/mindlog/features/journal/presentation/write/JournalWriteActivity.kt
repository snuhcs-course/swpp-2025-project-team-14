package com.example.mindlog.features.journal.presentation.write

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.semantics.setText
import androidx.lifecycle.lifecycleScope
import com.example.mindlog.R
import com.example.mindlog.core.common.Result
import com.example.mindlog.core.common.SystemUiHelper
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
    private var loadingDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJournalWriteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SystemUiHelper.hideSystemUI(this)

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
                // '작성' 버튼 여러 번 눌러서 중복 저장되는 것을 막기 위해 즉시 버튼 비활성화
                binding.btnNextOrSave.isEnabled = false
                binding.btnCancelOrBack.isEnabled = false
                // 저장 중임을 사용자에게 알리기 위해 화면을 흐리게 하고 로딩 스피너 표시
                showSavingDialog()
                // '작성' 버튼 클릭 시 ViewModel의 saveJournal 함수 호출
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
                        hideSavingDialog()
                        setResult(RESULT_OK)
                        Toast.makeText(this@JournalWriteActivity, "일기가 저장되었습니다.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    is Result.Error -> {
                        hideSavingDialog()
                        binding.btnNextOrSave.isEnabled = true
                        binding.btnCancelOrBack.isEnabled = true
                        val errorMessage = result.message ?: "알 수 없는 오류가 발생했습니다."

                        // ✨ [핵심 수정] '감사한 일' 칸에 에러를 쓰는 대신, Log.e로 에러를 기록하고 사용자에게는 간단한 토스트만 보여줍니다.
                        Log.e("JournalWriteError", "일기 저장 실패: $errorMessage")
                        Toast.makeText(this@JournalWriteActivity, errorMessage, Toast.LENGTH_LONG).show()
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
            setResult(RESULT_CANCELED)
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

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            SystemUiHelper.hideSystemUI(this)
        }
    }

    private fun showSavingDialog() {
        if (loadingDialog?.isShowing == true) return

        val progressBar = android.widget.ProgressBar(this)

        // 스피너 + 텍스트를 담는 세로 레이아웃
        val contentLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            val padding = (24 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
        }

        // 스피너 추가
        contentLayout.addView(
            progressBar,
            android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        // "일기를 저장하고 있습니다..." 문구 추가
        val messageView = android.widget.TextView(this).apply {
            text = "일기를 저장하고 있습니다..."
            setTextColor(android.graphics.Color.WHITE)
            textSize = 16f
            setPadding(0, (16 * resources.displayMetrics.density).toInt(), 0, 0)
        }

        contentLayout.addView(
            messageView,
            android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        // 전체 화면을 덮는 컨테이너 (자기 자신은 투명, dim 효과는 윈도우에서)
        val container = android.widget.FrameLayout(this).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            addView(
                contentLayout,
                android.widget.FrameLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                    android.view.Gravity.CENTER
                )
            )
        }

        loadingDialog = Dialog(this).apply {
            requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
            setCancelable(false)
            setContentView(container)
            window?.apply {
                // 까만 네모 없애기 위해 완전 투명
                setBackgroundDrawable(
                    android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
                )
                // 전체 화면 크기로
                setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                // 뒤 배경만 살짝 어둡게 (0.0~1.0)
                setDimAmount(0.6f)
            }
            show()
        }
    }

    private fun hideSavingDialog() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }
}

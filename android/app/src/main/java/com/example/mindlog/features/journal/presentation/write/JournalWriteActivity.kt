package com.example.mindlog.features.journal.presentation.write

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.mindlog.R
import com.example.mindlog.core.domain.Result
import com.example.mindlog.core.ui.SystemUiHelper
import com.example.mindlog.databinding.ActivityJournalWriteBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import androidx.core.graphics.drawable.toDrawable

@AndroidEntryPoint
class JournalWriteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityJournalWriteBinding

    private val emotionSelectFragment by lazy { EmotionSelectFragment() }
    private val contentWriteFragment by lazy { ContentWriteFragment() }

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
        observeViewModel()
    }

    private fun setupButtonClickListeners() {
        binding.btnCancelOrBack.setOnClickListener {
            checkImageGenerationBeforeAction {
                handleBackButton()
            }
        }

        binding.btnNextOrSave.setOnClickListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.write_fragment_container)

            if (currentFragment is EmotionSelectFragment) {
                if (viewModel.hasSelectedEmotions()) {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.write_fragment_container, contentWriteFragment)
                        .addToBackStack(null)
                        .commit()
                    updateButtonsForContentWrite()
                } else {
                    showEmotionSelectionRequiredDialog()
                }
            } else {
                checkImageGenerationBeforeAction {
                    saveJournal()
                }
            }
        }
    }
    private fun showEmotionSelectionRequiredDialog() {
        MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_MindLog_AlertDialog)
            .setTitle("감정 미선택")
            .setMessage("오늘의 감정을 하나 이상 선택해주세요.")
            .setPositiveButton("확인", null)
            .show()
    }

    private fun saveJournal() {
        binding.btnNextOrSave.isEnabled = false
        binding.btnCancelOrBack.isEnabled = false
        showSavingDialog()
        viewModel.saveJournal()
    }

    private fun checkImageGenerationBeforeAction(action: () -> Unit) {
        if (viewModel.isLoading.value) {
            MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_MindLog_AlertDialog)
                .setTitle("이미지 생성 중")
                .setMessage("이미지를 생성하고 있어요.\n지금 뒤로 가거나 저장하면 생성이 취소됩니다.\n계속하시겠습니까?")
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
                checkImageGenerationBeforeAction {
                    handleBackButton()
                }
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
        val contentLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            val padding = (24 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
        }
        contentLayout.addView(progressBar, android.widget.LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT))
        val messageView = android.widget.TextView(this).apply {
            text = "일기를 저장하고 있습니다..."
            setTextColor(android.graphics.Color.WHITE)
            textSize = 16f
            setPadding(0, (16 * resources.displayMetrics.density).toInt(), 0, 0)
        }
        contentLayout.addView(messageView, android.widget.LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT))
        val container = android.widget.FrameLayout(this).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            addView(contentLayout, android.widget.FrameLayout.LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.Gravity.CENTER))
        }
        loadingDialog = Dialog(this).apply {
            requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
            setCancelable(false)
            setContentView(container)
            window?.apply {
                setBackgroundDrawable(android.graphics.Color.TRANSPARENT.toDrawable())
                setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT)
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

package com.example.mindlog

import android.os.Bundle
import android.widget.Button
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

class JournalWriteActivity : BaseActivity() {

    private val emotionSelectFragment = EmotionSelectFragment()
    private val contentWriteFragment = ContentWriteFragment()

    private lateinit var btnCancelOrBack: Button
    private lateinit var btnNextOrSave: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_journal_write)

        btnCancelOrBack = findViewById(R.id.btn_cancel_or_back)
        btnNextOrSave = findViewById(R.id.btn_next_or_save)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.write_fragment_container, emotionSelectFragment)
                .commit()
            updateButtonsForEmotionSelect()
        }

        setupButtonClickListeners()

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentFragment = supportFragmentManager.findFragmentById(R.id.write_fragment_container)
                if (currentFragment is ContentWriteFragment) {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.write_fragment_container, emotionSelectFragment)
                        .commit()
                    updateButtonsForEmotionSelect()
                } else {
                    finish()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }

    private fun setupButtonClickListeners() {
        btnCancelOrBack.setOnClickListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.write_fragment_container)
            if (currentFragment is ContentWriteFragment) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.write_fragment_container, emotionSelectFragment)
                    .commit()
                updateButtonsForEmotionSelect()
            } else {
                finish()
            }
        }

        btnNextOrSave.setOnClickListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.write_fragment_container)
            if (currentFragment is EmotionSelectFragment) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.write_fragment_container, contentWriteFragment)
                    .commit()
                updateButtonsForContentWrite()
            } else {
                // TODO: 여기에 작성된 일기 데이터를 데이터베이스에 저장하는 로직을 추가해야 합니다.
                finish()
            }
        }
    }

    private fun updateButtonsForEmotionSelect() {
        btnCancelOrBack.text = "취소"
        btnNextOrSave.text = "다음"
    }

    private fun updateButtonsForContentWrite() {
        btnCancelOrBack.text = "뒤로"
        btnNextOrSave.text = "작성"
    }
}

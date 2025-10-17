package com.example.mindlog

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class JournalWriteActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_journal_write)

        // Activity가 처음 생성될 때만 초기 프래그먼트(감정 선택 화면)를 표시합니다.
        // 화면 회전 등 상태 변경 시에는 프래그먼트가 중복으로 추가되는 것을 방지합니다.
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.write_fragment_container, EmotionSelectFragment())
                .commit()
        }
    }
}

package com.example.mindlog.features.home.presentation

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.mindlog.R
import com.example.mindlog.core.common.SystemUiHelper
import com.example.mindlog.databinding.ActivityHomeBinding
import com.example.mindlog.features.journal.presentation.write.JournalWriteActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SystemUiHelper.hideSystemUI(this)

        val navHost =
            supportFragmentManager.findFragmentById(R.id.homeNavHost) as NavHostFragment
        val navController = navHost.navController

        // BottomNav <-> NavController 연결
        binding.bottomNav.apply {
            setupWithNavController(navController)
            setOnItemReselectedListener { /* 탭 재선택시 스크롤 탑 등 필요하면 처리 */ }
        }

        binding.fabWrite.setOnClickListener {
            Toast.makeText(this, "일기 작성 버튼 클릭!", Toast.LENGTH_SHORT).show()
        }

        binding.fabWrite.setOnClickListener {
            val intent = Intent(this, JournalWriteActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            SystemUiHelper.hideSystemUI(this)
        }
    }
}
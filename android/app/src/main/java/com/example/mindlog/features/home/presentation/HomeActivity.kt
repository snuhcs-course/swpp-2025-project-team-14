package com.example.mindlog.features.home.presentation

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.mindlog.R
import com.example.mindlog.databinding.ActivityHomeBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        /*
        // FAB: 일기 작성 화면으로
        binding.fabWrite.setOnClickListener {
            if (navController.currentDestination?.id != R.id.writeJournalFragment) {
                navController.navigate(R.id.writeJournalFragment)
            }
        }

        // 작성 화면일 때 하단바/FAB 숨기기
        navController.addOnDestinationChangedListener { _, dest, _ ->
            val isWrite = dest.id == R.id.writeJournalFragment
            binding.bottomBarContainer.isVisible = !isWrite
            binding.fabWrite.isVisible = !isWrite
        }
        */
    }
}
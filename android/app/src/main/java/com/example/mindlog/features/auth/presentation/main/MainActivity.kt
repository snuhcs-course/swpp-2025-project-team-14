package com.example.mindlog.features.auth.presentation.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.mindlog.core.ui.SystemUiHelper
import com.example.mindlog.databinding.ActivityMainBinding
import com.example.mindlog.features.auth.presentation.login.LoginActivity
import com.example.mindlog.features.home.presentation.HomeActivity
import com.example.mindlog.features.tutorial.TutorialActivity
import com.example.mindlog.features.tutorial.TutorialMenuActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SystemUiHelper.hideSystemUI(this)

        observeState()

        val completed = hasCompletedTutorial()
        viewModel.checkAutoLogin(completed)
    }

    private fun observeState() {
        viewModel.state.observe(this) { state ->
            when (state) {
                MainState.GoLogin -> goToLogin()
                MainState.GoTutorial -> goToTutorial()
                MainState.GoHome -> goToJournal()
            }
        }
    }

    private fun hasCompletedTutorial(): Boolean {
        val prefs = getSharedPreferences("tutorial_prefs", MODE_PRIVATE)
        return prefs.getBoolean("completed", false)
    }

    private fun goToTutorial() {
        val intent = Intent(this, TutorialActivity::class.java).apply {
            putExtra(TutorialActivity.EXTRA_GO_TO_MENU, true)
        }
        startActivity(intent)
        finish()
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun goToJournal() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}
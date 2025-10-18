package com.example.mindlog

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlin.io.encoding.Base64

class MainActivity : BaseActivity() {
    private val diaryFragment = DiaryFragment()
    private val selfAwareFragment = SelfAwareFragment()
    private val statsFragment = StatsFragment()
    private val analysisFragment = AnalysisFragment()
    private lateinit var activeFragment: Fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            setupInitialFragments()
        }

        setupBottomNavigation()
    }

    private fun setupInitialFragments() {
        supportFragmentManager.beginTransaction().apply {
            add(R.id.fragment_container, analysisFragment, "4").hide(analysisFragment)
            add(R.id.fragment_container, statsFragment, "3").hide(statsFragment)
            add(R.id.fragment_container, selfAwareFragment, "2").hide(selfAwareFragment)
            add(R.id.fragment_container, diaryFragment, "1")
        }.commit()
        activeFragment = diaryFragment
    }


    private fun setupBottomNavigation() {
        val bottomNavLayout = findViewById<CoordinatorLayout>(R.id.bottom_nav_layout)
        val diaryButton = bottomNavLayout.findViewById<LinearLayout>(R.id.item_diary)
        val selfAwareButton = bottomNavLayout.findViewById<LinearLayout>(R.id.item_me)
        val statsButton = bottomNavLayout.findViewById<LinearLayout>(R.id.item_stats)
        val analysisButton = bottomNavLayout.findViewById<LinearLayout>(R.id.item_analysis)
        val fabWrite = bottomNavLayout.findViewById<FloatingActionButton>(R.id.fab_write)

        diaryButton.setOnClickListener {
            switchFragment(diaryFragment)
        }

        selfAwareButton.setOnClickListener {
            switchFragment(selfAwareFragment)
        }

        statsButton.setOnClickListener {
            switchFragment(statsFragment)
        }

        analysisButton.setOnClickListener {
            switchFragment(analysisFragment)
        }

        fabWrite.setOnClickListener {
            val intent = Intent(this, JournalWriteActivity::class.java)
            startActivity(intent)
        }
    }

    private fun switchFragment(targetFragment: Fragment) {
        if (targetFragment == activeFragment) return

        supportFragmentManager.beginTransaction()
            .hide(activeFragment)
            .show(targetFragment)
            .commit()
        activeFragment = targetFragment
    }
}

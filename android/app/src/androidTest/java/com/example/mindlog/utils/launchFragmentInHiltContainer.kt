package com.example.mindlog.utils

import android.content.ComponentName
import android.content.Intent
import androidx.annotation.StyleRes
import androidx.fragment.app.Fragment
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.mindlog.HiltTestActivity
import com.example.mindlog.R

inline fun <reified F : Fragment> launchFragmentInHiltContainer(
    crossinline factory: () -> F,
    @StyleRes themeResId: Int = R.style.Theme_MindLog,
    fragmentTag: String = "TEST"
) {
    val intent = Intent.makeMainActivity(
        ComponentName(
            ApplicationProvider.getApplicationContext(),
            HiltTestActivity::class.java
        )
    ).putExtra("theme", themeResId)

    ActivityScenario.launch<HiltTestActivity>(intent).onActivity { activity ->
        val fragment = factory()
        activity.setTheme(themeResId)
        activity.supportFragmentManager
            .beginTransaction()
            .replace(android.R.id.content, fragment, fragmentTag)
            .commitNow()
    }
}
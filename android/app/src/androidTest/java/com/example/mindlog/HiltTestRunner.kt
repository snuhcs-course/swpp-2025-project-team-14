package com.example.mindlog

import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, name: String?, context: Context?) =
        super.newApplication(cl, "dagger.hilt.android.testing.HiltTestApplication", context)
}
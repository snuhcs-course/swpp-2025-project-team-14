package com.example.mindlog.features.journal.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class JournalFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return TextView(requireContext()).apply {
            text = "Journal 화면 (준비 중)"
            textSize = 18f
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            setPadding(0, 400, 0, 0)
        }
    }
}
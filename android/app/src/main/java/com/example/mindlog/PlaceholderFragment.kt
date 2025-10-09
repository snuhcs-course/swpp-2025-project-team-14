package com.example.mindlog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class PlaceholderFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val tv = TextView(requireContext())
        tv.text = requireArguments().getString("title", "Placeholder")
        tv.textSize = 20f
        return tv
    }
    companion object {
        fun new(title: String) = PlaceholderFragment().apply {
            arguments = Bundle().apply { putString("title", title) }
        }
    }
}

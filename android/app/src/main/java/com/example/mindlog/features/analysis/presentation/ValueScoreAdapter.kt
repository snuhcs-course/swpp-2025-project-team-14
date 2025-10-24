package com.example.mindlog.features.analysis.presentation

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mindlog.R
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textview.MaterialTextView

class ValueScoreAdapter(
    private val onBindColor: (position: Int) -> Int // 각 항목 색상
) : ListAdapter<ValueScoreItem, ValueScoreAdapter.VH>(diff) {

    object diff : DiffUtil.ItemCallback<ValueScoreItem>() {
        override fun areItemsTheSame(a: ValueScoreItem, b: ValueScoreItem) = a.value == b.value
        override fun areContentsTheSame(a: ValueScoreItem, b: ValueScoreItem) = a == b
    }

    inner class VH(val root: View) : RecyclerView.ViewHolder(root) {
        val tvValue = root.findViewById<MaterialTextView>(R.id.tvValue)
        val tvScore = root.findViewById<MaterialTextView>(R.id.tvScore)
        val progress = root.findViewById<LinearProgressIndicator>(R.id.progress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_value_score, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        val item = getItem(position)
        h.tvValue.text = item.value
        h.tvScore.text = item.score.toInt().toString()

        h.progress.max = 100
        h.progress.progress = item.score.toInt()

        // 색상 지정 (막대/트랙)
        val bar = onBindColor(position)
        val track = Color.parseColor("#F1EDE4") // 옅은 아이보리
        h.progress.setIndicatorColor(bar)
        h.progress.trackColor = track
    }
}
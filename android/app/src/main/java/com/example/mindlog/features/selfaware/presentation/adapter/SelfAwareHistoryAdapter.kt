package com.example.mindlog.features.selfaware.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mindlog.databinding.ItemQaHistoryBinding
import com.google.android.material.chip.Chip
import com.example.mindlog.features.selfaware.domain.model.QAItem

/**
 * SelfAware의 답변 기록(Answer History) RecyclerView Adapter
 */
class SelfAwareHistoryAdapter :
    ListAdapter<QAItem, SelfAwareHistoryAdapter.ViewHolder>(Diff) {

    inner class ViewHolder(
        private val binding: ItemQaHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: QAItem) = with(binding) {
            tvDate.text = item.answer.createdAt.toString()
            tvQuestion.text = item.question.text
            tvAnswer.text = item.answer.text

            // 가치 칩들 표시 (value_scores 기반)
            /*
            chipGroupValues.removeAllViews()
            val scores = item.answer.valueScores.take(4) // 상위 4개만 표시
            scores.forEach { score ->
                chipGroupValues.addView(createChip(score))
            }
            */
        }

        /*
        private fun createChip(score: ValueScore): Chip {
            val chip = Chip(binding.root.context)
            chip.text = score.value
            chip.isClickable = false
            chip.isCheckable = false
            chip.setTextColor(0xFF2D3142.toInt())
            chip.chipCornerRadius = 18f
            chip.chipMinHeight = 36f
            chip.setChipBackgroundColorResource(com.minglog.core.R.color.mindlog_chip_bg_beige)
            return chip
        }
         */
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemQaHistoryBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        val Diff = object : DiffUtil.ItemCallback<QAItem>() {
            override fun areItemsTheSame(oldItem: QAItem, newItem: QAItem) =
                oldItem.answer.id == newItem.answer.id

            override fun areContentsTheSame(oldItem: QAItem, newItem: QAItem) =
                oldItem == newItem
        }
    }
}
package com.example.mindlog.features.tutorial.presentation

import androidx.recyclerview.widget.RecyclerView
import com.example.mindlog.R
import com.example.mindlog.databinding.ItemTutorialFeatureBinding
import com.example.mindlog.features.tutorial.domain.model.TutorialFeature

class TutorialFeatureViewHolder(
    private val binding: ItemTutorialFeatureBinding,
    private val onClick: (TutorialFeature) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: TutorialFeature) {
        val iconRes = when (item.label) {
            "일기 작성" -> R.drawable.ic_journal
            "일기 상세 보기 및 수정" -> R.drawable.ic_detail
            "일기 검색" -> R.drawable.ic_search
            "질문을 통해 나 알아가기" -> R.drawable.ic_selfaware
            "일기 통계" -> R.drawable.ic_stats
            "나에 대한 분석" -> R.drawable.ic_analysis
            "설정" -> R.drawable.ic_settings
            else -> R.drawable.ic_settings
        }

        binding.ivFeatureIcon.setImageResource(iconRes)
        binding.tvFeatureTitle.text = item.label
        binding.tvFeatureDescription.text = item.description
        binding.root.setOnClickListener { onClick(item) }
    }
}
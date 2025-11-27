package com.example.mindlog.features.tutorial.presentation

import androidx.recyclerview.widget.RecyclerView
import com.example.mindlog.databinding.ItemTutorialFeatureBinding
import com.example.mindlog.features.tutorial.domain.model.TutorialFeature

class TutorialFeatureViewHolder(
    private val binding: ItemTutorialFeatureBinding,
    private val onClick: (TutorialFeature) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: TutorialFeature) {
        binding.tvFeatureTitle.text = item.label
        binding.tvFeatureDescription.text = item.description
        binding.root.setOnClickListener { onClick(item) }
    }
}
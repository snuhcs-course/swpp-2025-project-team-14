package com.example.mindlog.features.tutorial.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mindlog.databinding.ItemTutorialFeatureBinding
import com.example.mindlog.features.tutorial.domain.model.TutorialFeature

class TutorialFeatureAdapter(
    private val items: List<TutorialFeature>,
    private val onClick: (TutorialFeature) -> Unit
) : RecyclerView.Adapter<TutorialFeatureViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TutorialFeatureViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemTutorialFeatureBinding.inflate(inflater, parent, false)
        return TutorialFeatureViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: TutorialFeatureViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}

package com.example.mindlog.features.tutorial.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mindlog.databinding.ItemTutorialPageBinding
import com.example.mindlog.features.tutorial.domain.model.TutorialPage

class TutorialAdapter(
    private val pages: List<TutorialPage>
) : RecyclerView.Adapter<TutorialAdapter.TutorialViewHolder>() {

    inner class TutorialViewHolder(
        private val binding: ItemTutorialPageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(page: TutorialPage) {
            binding.ivTutorial.setImageResource(page.imageRes)
            // page.title을 따로 쓰고 싶으면 여기서 TextView 추가해서 바인딩
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TutorialViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemTutorialPageBinding.inflate(inflater, parent, false)
        return TutorialViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TutorialViewHolder, position: Int) {
        holder.bind(pages[position])
    }

    override fun getItemCount(): Int = pages.size
}
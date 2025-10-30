package com.example.mindlog.features.journal.presentation.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy // ✨ [추가]
import com.example.mindlog.core.model.JournalEntry
import com.example.mindlog.databinding.ItemJournalCardBinding
import com.example.mindlog.features.journal.presentation.write.JournalEditActivity
import java.text.SimpleDateFormat
import java.util.Locale

class JournalAdapter : ListAdapter<JournalEntry, JournalAdapter.ViewHolder>(JournalDiffCallback) {

    private val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일 E요일 HH:mm", Locale.getDefault())

    inner class ViewHolder(private val binding: ItemJournalCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(journal: JournalEntry) {
            binding.tvTitle.text = journal.title
            binding.tvBodyPreview.text = journal.content
            binding.tvDate.text = dateFormat.format(journal.createdAt)

            if (journal.imageUrl != null) {
                binding.ivThumbnail.visibility = View.VISIBLE

                // ✨ [핵심 수정] 캐시 전략을 추가하여 네트워크에서 새로 받아오도록 합니다.
                Glide.with(itemView.context)
                    .asBitmap()
                    .load(journal.imageUrl)
                    // 👇 이 두 줄을 추가하여 캐시를 무시하고 새로 다운로드 받도록 강제합니다.
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(binding.ivThumbnail)

            } else {
                binding.ivThumbnail.visibility = View.GONE
            }

            itemView.setOnClickListener {
                val context = itemView.context
                val intent = Intent(context, JournalEditActivity::class.java).apply {
                    putExtra(JournalEditActivity.EXTRA_JOURNAL_ID, journal.id)
                }
                context.startActivity(intent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemJournalCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private object JournalDiffCallback : DiffUtil.ItemCallback<JournalEntry>() {
        override fun areItemsTheSame(oldItem: JournalEntry, newItem: JournalEntry): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: JournalEntry, newItem: JournalEntry): Boolean {
            return oldItem == newItem
        }
    }
}

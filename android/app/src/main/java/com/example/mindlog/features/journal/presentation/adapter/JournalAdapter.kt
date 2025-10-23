package com.example.mindlog.features.journal.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mindlog.core.model.JournalEntry
import com.example.mindlog.databinding.ItemJournalCardBinding
import java.text.SimpleDateFormat
import java.util.Locale

class JournalAdapter : ListAdapter<JournalEntry, JournalAdapter.ViewHolder>(JournalDiffCallback) {

    // SimpleDateFormat은 생성 비용이 비싸므로 한번만 만드는 것이 좋음
    private val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일 E요일 HH:mm", Locale.getDefault())

    inner class ViewHolder(private val binding: ItemJournalCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(journal: JournalEntry) {
            // --- item_journal_card.xml의 ID에 맞춰 데이터 바인딩 ---
            binding.tvTitle.text = journal.title
            binding.tvBodyPreview.text = journal.content // 본문 내용은 tv_body_preview에 바인딩
            binding.tvDate.text = dateFormat.format(journal.createdAt) // 날짜 포맷 적용

            // (선택사항) 아이템 클릭 리스너 설정
            itemView.setOnClickListener {
                // TODO: 아이템 클릭 시 일기 상세 페이지로 이동하는 로직 구현
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

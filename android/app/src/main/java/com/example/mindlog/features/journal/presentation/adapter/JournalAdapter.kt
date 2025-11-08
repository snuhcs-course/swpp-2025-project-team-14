package com.example.mindlog.features.journal.presentation.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.layout.layout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.mindlog.R
import com.example.mindlog.core.model.JournalEntry
import com.example.mindlog.databinding.ItemJournalCardBinding
import com.example.mindlog.features.journal.presentation.detail.JournalDetailActivity
import com.example.mindlog.features.journal.presentation.write.JournalEditActivity
import java.text.SimpleDateFormat
import java.util.Locale

class JournalAdapter : ListAdapter<JournalEntry, JournalAdapter.ViewHolder>(JournalDiffCallback) {

    private val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일 E요일", Locale.getDefault())
    inner class ViewHolder(private val binding: ItemJournalCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(journal: JournalEntry) {
            binding.tvTitle.text = journal.title
            binding.tvBodyPreview.text = journal.content
            binding.tvDate.text = dateFormat.format(journal.createdAt)

            if (journal.imageUrl != null) {
                binding.ivThumbnail.visibility = View.VISIBLE

                Glide.with(itemView.context)
                    .asBitmap()
                    .load(journal.imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(binding.ivThumbnail)

            } else {
                binding.ivThumbnail.visibility = View.GONE
            }

            val flexbox = binding.flexboxKeywords
            if (journal.keywords.isNotEmpty()) {
                flexbox.visibility = View.VISIBLE
                flexbox.removeAllViews()

                journal.keywords.forEach { keyword ->
                    // ✨ [수정] Chip을 TextView로 변경하고 캐스팅 제거
                    val keywordView = LayoutInflater.from(itemView.context)
                        .inflate(R.layout.item_keyword_chip, flexbox, false) as android.widget.TextView

                    keywordView.text = "#${keyword.keyword}"
                    // isClickable 설정은 이제 필요 없음

                    flexbox.addView(keywordView)
                }
            } else {
                flexbox.visibility = View.GONE
            }

            itemView.setOnClickListener {
                val context = itemView.context
                val intent = Intent(context, JournalDetailActivity::class.java).apply {
                    putExtra(JournalDetailActivity.EXTRA_JOURNAL_ID, journal.id)
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

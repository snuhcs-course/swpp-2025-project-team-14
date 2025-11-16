package com.example.mindlog.features.journal.presentation.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.text
import androidx.core.content.ContextCompat
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
    inner class ViewHolder(private val binding: ItemJournalCardBinding) : RecyclerView.ViewHolder(binding.root) {        fun bind(journal: JournalEntry) {
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

        // ✨ [핵심 수정] 감정 데이터 처리
        val emotionsFlexbox = binding.flexboxEmotions
        val filteredEmotions = journal.emotions
            .filter { it.intensity >= 3 } // intensity 3 이상인 감정만 필터링
            .sortedByDescending { it.intensity } // intensity 높은 순으로 정렬 (4 -> 3)

        if (filteredEmotions.isNotEmpty()) {
            emotionsFlexbox.visibility = View.VISIBLE
            emotionsFlexbox.removeAllViews()

            filteredEmotions.forEach { emotion ->
                val emotionView = LayoutInflater.from(itemView.context)
                    .inflate(R.layout.item_keyword_chip, emotionsFlexbox, false) as TextView

                // ✨ [수정] 한글 이름으로 텍스트 설정
                emotionView.text = getKoreanEmotionName(emotion.emotion)
                // ✨ [수정] intensity에 따라 색상 설정
                emotionView.setTextColor(getEmotionColor(emotion.emotion, emotion.intensity))

                if (emotion.intensity == 4) {
                    emotionView.setTypeface(null, android.graphics.Typeface.BOLD)
                } else {
                    emotionView.setTypeface(null, android.graphics.Typeface.NORMAL)
                }

                emotionsFlexbox.addView(emotionView)
            }
        } else {
            emotionsFlexbox.visibility = View.GONE
        }

        // 키워드 데이터 처리 (기존과 동일)
        val keywordsFlexbox = binding.flexboxKeywords
        if (journal.keywords.isNotEmpty()) {
            keywordsFlexbox.visibility = View.VISIBLE
            keywordsFlexbox.removeAllViews()

            journal.keywords.forEach { keyword ->
                val keywordView = LayoutInflater.from(itemView.context)
                    .inflate(R.layout.item_keyword_chip, keywordsFlexbox, false) as TextView
                keywordView.text = "#${keyword.keyword}"
                keywordsFlexbox.addView(keywordView)
            }
        } else {
            keywordsFlexbox.visibility = View.GONE
        }

        itemView.setOnClickListener {
            val context = itemView.context
            val intent = Intent(context, JournalDetailActivity::class.java).apply {
                putExtra(JournalDetailActivity.EXTRA_JOURNAL_ID, journal.id)
            }
            context.startActivity(intent)
        }
    }

        // ✨ [핵심 추가] API 이름을 한글로 변환하는 함수
        private fun getKoreanEmotionName(apiName: String): String {
            return when (apiName) {
                "happy" -> "행복"
                "sad" -> "슬픔"
                "anxious" -> "불안"
                "calm" -> "편안"
                "annoyed" -> "짜증"
                "satisfied" -> "만족"
                "bored" -> "지루함"
                "interested" -> "흥미"
                "lethargic" -> "무기력"
                "energetic" -> "활력"
                else -> apiName
            }
        }

        private fun getEmotionColor(emotionName: String, intensity: Int): Int {
            val colorResId = when (emotionName.lowercase()) {
                "happy" -> R.color.emotion_happy
                "sad" -> R.color.emotion_sad
                "anxious" -> R.color.emotion_anxious
                "calm" -> R.color.emotion_calm
                "annoyed" -> R.color.emotion_annoyed
                "satisfied" -> R.color.emotion_satisfied
                "bored" -> R.color.emotion_bored
                "interested" -> R.color.emotion_interested
                "lethargic" -> R.color.emotion_lethargic
                "energetic" -> R.color.emotion_energetic
                else -> R.color.text_secondary
            }
            val targetColor = ContextCompat.getColor(itemView.context, colorResId)

            // ✨ intensity가 3이면 흰색과 섞어서 연하게 만듦
            if (intensity == 3) {
                val whiteColor = ContextCompat.getColor(itemView.context, R.color.white)
                return interpolateColor(targetColor, whiteColor, 0.5f)
            }

            return targetColor
        }

        private fun interpolateColor(color1: Int, color2: Int, ratio: Float): Int {
            val inverseRatio = 1 - ratio
            val a = (android.graphics.Color.alpha(color1) * inverseRatio + android.graphics.Color.alpha(color2) * ratio).toInt()
            val r = (android.graphics.Color.red(color1) * inverseRatio + android.graphics.Color.red(color2) * ratio).toInt()
            val g = (android.graphics.Color.green(color1) * inverseRatio + android.graphics.Color.green(color2) * ratio).toInt()
            val b = (android.graphics.Color.blue(color1) * inverseRatio + android.graphics.Color.blue(color2) * ratio).toInt()
            return android.graphics.Color.argb(a, r, g, b)
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

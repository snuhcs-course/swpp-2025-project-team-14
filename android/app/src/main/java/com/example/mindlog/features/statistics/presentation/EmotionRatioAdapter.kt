package com.example.mindlog.features.statistics.presentation

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mindlog.R
import com.example.mindlog.features.statistics.domain.model.EmotionRatio
import com.example.mindlog.features.statistics.widget.TwoToneRatioBar
import com.google.android.material.textview.MaterialTextView

data class EmotionRatioPair(
    val leftEmotion: EmotionRatio,
    val rightEmotion: EmotionRatio
)
class EmotionRatioAdapter : ListAdapter<EmotionRatioPair, EmotionRatioAdapter.VH>(diff) {
    object diff : DiffUtil.ItemCallback<EmotionRatioPair>() {
        override fun areItemsTheSame(a: EmotionRatioPair, b: EmotionRatioPair) = a.leftEmotion == b.leftEmotion && a.rightEmotion == b.rightEmotion
        override fun areContentsTheSame(a: EmotionRatioPair, b: EmotionRatioPair) = a == b
    }
    inner class VH(v: View): RecyclerView.ViewHolder(v) {
        val tvLeft = v.findViewById<MaterialTextView>(R.id.tvLeftEmotion)
        val tvRight = v.findViewById<MaterialTextView>(R.id.tvRightEmotion)
        val tvLeftPct = v.findViewById<MaterialTextView>(R.id.tvLeftRatio)
        val tvRightPct = v.findViewById<MaterialTextView>(R.id.tvRightRatio)
        val bar = v.findViewById<TwoToneRatioBar>(R.id.bar)
    }
    override fun onCreateViewHolder(p: ViewGroup, vt: Int) =
        VH(LayoutInflater.from(p.context).inflate(R.layout.item_emotion_ratio, p, false))

    override fun onBindViewHolder(h: VH, pos: Int) {
        val item = getItem(pos)
        h.tvLeft.text = item.leftEmotion.emotion
        h.tvRight.text = item.rightEmotion.emotion
        h.tvLeftPct.text = "${item.leftEmotion.ratio.toInt()}%"
        h.tvRightPct.text = "${item.rightEmotion.ratio.toInt()}%"

        // 색 팔레트(행별로 바꾸고 싶으면 pos % size 로 순환)
        val leftColors  = listOf("#4D9BFF","#8A42D6","#E25544","#9EA3AA","#6A4C3B").map(Color::parseColor)
        val rightColors = listOf("#FFCD3C","#5EBB6A","#97C75B","#4AC0D6","#FF7A3C").map(Color::parseColor)

        h.bar.apply {
            leftPercent = item.leftEmotion.ratio
            rightPercent = item.rightEmotion.ratio
            leftColor = leftColors[pos % leftColors.size]
            rightColor = rightColors[pos % rightColors.size]
            trackColor = Color.parseColor("#F1EDE4")
        }
    }
}
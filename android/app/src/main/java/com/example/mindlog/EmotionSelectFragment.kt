package com.example.mindlog

import android.content.res.ColorStateList
import android.graphics.Color // Color 클래스 import 추가
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class EmotionSelectFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_emotion_select, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rowSadHappy = view.findViewById<View>(R.id.row_sad_happy)
        val rowAnxiousConfident = view.findViewById<View>(R.id.row_anxious_confident)
        val rowAngryCalm = view.findViewById<View>(R.id.row_angry_calm)
        val rowBoredExcited = view.findViewById<View>(R.id.row_bored_excited)
        val rowLethargicEnergetic = view.findViewById<View>(R.id.row_lethargic_energetic)

        setupEmotionRow(
            rowView = rowSadHappy,
            leftText = "슬픔",
            rightText = "행복",
            leftColorRes = R.color.emotion_sad_blue,
            rightColorRes = R.color.emotion_happy_orange
        )

        setupEmotionRow(
            rowView = rowAnxiousConfident,
            leftText = "불안",
            rightText = "자신감",
            leftColorRes = R.color.emotion_anxious_purple,
            rightColorRes = R.color.emotion_confident_green
        )

        setupEmotionRow(
            rowView = rowAngryCalm,
            leftText = "짜증/분노",
            rightText = "편안",
            leftColorRes = R.color.emotion_angry_red,
            rightColorRes = R.color.emotion_calm_green
        )

        setupEmotionRow(
            rowView = rowBoredExcited,
            leftText = "지루함",
            rightText = "흥미/설렘",
            leftColorRes = R.color.emotion_bored_blue,
            rightColorRes = R.color.emotion_excited_pink
        )

        setupEmotionRow(
            rowView = rowLethargicEnergetic,
            leftText = "무기력",
            rightText = "활력",
            leftColorRes = R.color.emotion_lethargic_indigo,
            rightColorRes = R.color.emotion_happy_orange
        )
    }

    private fun setupEmotionRow(
        rowView: View,
        leftText: String,
        rightText: String,
        leftColorRes: Int,
        rightColorRes: Int
    ) {
        val tvLeft = rowView.findViewById<TextView>(R.id.tv_emotion_left)
        val tvRight = rowView.findViewById<TextView>(R.id.tv_emotion_right)
        val rbLeft = rowView.findViewById<RadioButton>(R.id.rb_left)
        val rbMidLeft = rowView.findViewById<RadioButton>(R.id.rb_mid_left)
        val rbCenter = rowView.findViewById<RadioButton>(R.id.rb_center)
        val rbMidRight = rowView.findViewById<RadioButton>(R.id.rb_mid_right)
        val rbRight = rowView.findViewById<RadioButton>(R.id.rb_right)

        val leftColor = ContextCompat.getColor(requireContext(), leftColorRes)
        val rightColor = ContextCompat.getColor(requireContext(), rightColorRes)
        val centerGrayColor =
            ContextCompat.getColor(requireContext(), R.color.emotion_lethargic_gray)

        tvLeft.text = leftText
        tvLeft.setTextColor(leftColor)
        tvRight.text = rightText
        tvRight.setTextColor(rightColor)

        rbLeft.backgroundTintList = ColorStateList.valueOf(leftColor)
        rbCenter.backgroundTintList = ColorStateList.valueOf(centerGrayColor)
        rbRight.backgroundTintList = ColorStateList.valueOf(rightColor)

        rbMidLeft.backgroundTintList =
            ColorStateList.valueOf(interpolateColor(leftColor, centerGrayColor, 0.5f))
        rbMidRight.backgroundTintList =
            ColorStateList.valueOf(interpolateColor(centerGrayColor, rightColor, 0.5f))
    }

    private fun interpolateColor(color1: Int, color2: Int, ratio: Float): Int {
        val inverseRatio = 1 - ratio
        val a = (Color.alpha(color1) * inverseRatio + Color.alpha(color2) * ratio).toInt()
        val r = (Color.red(color1) * inverseRatio + Color.red(color2) * ratio).toInt()
        val g = (Color.green(color1) * inverseRatio + Color.green(color2) * ratio).toInt()
        val b = (Color.blue(color1) * inverseRatio + Color.blue(color2) * ratio).toInt()
        return Color.argb(a, r, g, b)
    }
}

package com.example.mindlog

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

        // 각 include 된 레이아웃의 ID로 루트 뷰를 찾습니다.
        val rowSadHappy = view.findViewById<View>(R.id.row_sad_happy)
        val rowAnxiousConfident = view.findViewById<View>(R.id.row_anxious_confident)
        val rowAngryCalm = view.findViewById<View>(R.id.row_angry_calm)
        val rowBoredExcited = view.findViewById<View>(R.id.row_bored_excited)
        val rowLethargicEnergetic = view.findViewById<View>(R.id.row_lethargic_energetic)

        // 각 행에 맞는 데이터로 UI를 설정합니다.
        // 1행: 슬픔 - 행복
        setupEmotionRow(
            rowView = rowSadHappy,
            leftText = "슬픔",
            rightText = "행복",
            leftColorRes = R.color.emotion_sad_blue,
            rightColorRes = R.color.emotion_happy_orange,
            // 중간 색상들도 여기에 정의할 수 있습니다.
            midColors = listOf(0xFFB9CADB.toInt(), 0xFFCACACA.toInt(), 0xFFDBCDC0.toInt())
        )

        // 2행: 불안 - 자신감
        setupEmotionRow(
            rowView = rowAnxiousConfident,
            leftText = "불안",
            rightText = "자신감",
            leftColorRes = R.color.emotion_anxious_purple,
            rightColorRes = R.color.emotion_calm_green, // 자신감 색상 (calm_green으로 임시 사용)
            midColors = listOf(0xFFC0BEEB.toInt(), 0xFFCACACA.toInt(), 0xFFB9EBD2.toInt())
        )

        // 3행: 짜증/분노 - 편안
        setupEmotionRow(
            rowView = rowAngryCalm,
            leftText = "짜증/분노",
            rightText = "편안",
            leftColorRes = R.color.emotion_angry_red,
            rightColorRes = R.color.emotion_calm_green,
            midColors = listOf(0xFFEBC1C1.toInt(), 0xFFCACACA.toInt(), 0xFFB9EBD2.toInt())
        )

        // 4행: 지루함 - 흥미/설렘
        setupEmotionRow(
            rowView = rowBoredExcited,
            leftText = "지루함",
            rightText = "흥미/설렘",
            leftColorRes = R.color.emotion_lethargic_gray, // 지루함 색상 (lethargic_gray로 임시 사용)
            rightColorRes = R.color.emotion_excited_pink,
            midColors = listOf(0xFFD1D1D1.toInt(), 0xFFCACACA.toInt(), 0xFFEBC1D1.toInt())
        )

        // 5행: 무기력 - 활력
        setupEmotionRow(
            rowView = rowLethargicEnergetic,
            leftText = "무기력",
            rightText = "활력",
            leftColorRes = R.color.emotion_lethargic_gray,
            rightColorRes = R.color.emotion_happy_orange, // 활력 색상 (happy_orange로 임시 사용)
            midColors = listOf(0xFFD1D1D1.toInt(), 0xFFCACACA.toInt(), 0xFFDBCDC0.toInt())
        )
    }

    /**
     * 재사용되는 감정 행(item_emotion_row)의 UI를 설정하는 함수
     */
    private fun setupEmotionRow(
        rowView: View,
        leftText: String,
        rightText: String,
        leftColorRes: Int,
        rightColorRes: Int,
        midColors: List<Int>
    ) {
        // rowView (LinearLayout) 안에서 각 UI 요소를 찾습니다.
        val tvLeft = rowView.findViewById<TextView>(R.id.tv_emotion_left)
        val tvRight = rowView.findViewById<TextView>(R.id.tv_emotion_right)
        val rbLeft = rowView.findViewById<RadioButton>(R.id.rb_left)
        val rbMidLeft = rowView.findViewById<RadioButton>(R.id.rb_mid_left)
        val rbCenter = rowView.findViewById<RadioButton>(R.id.rb_center)
        val rbMidRight = rowView.findViewById<RadioButton>(R.id.rb_mid_right)
        val rbRight = rowView.findViewById<RadioButton>(R.id.rb_right)

        // ContextCompat.getColor를 사용하여 컬러 리소스를 실제 색상 값으로 변환
        val leftColor = ContextCompat.getColor(requireContext(), leftColorRes)
        val rightColor = ContextCompat.getColor(requireContext(), rightColorRes)

        // 텍스트와 텍스트 색상 설정
        tvLeft.text = leftText
        tvLeft.setTextColor(leftColor)
        tvRight.text = rightText
        tvRight.setTextColor(rightColor)

        // 각 라디오 버튼의 배경 색상(Tint) 설정
        rbLeft.backgroundTintList = ContextCompat.getColorStateList(requireContext(), leftColorRes)
        rbRight.backgroundTintList = ContextCompat.getColorStateList(requireContext(), rightColorRes)

        // 중간 색상 설정
        if (midColors.size == 3) {
            rbMidLeft.background.setTint(midColors[0])
            rbCenter.background.setTint(midColors[1])
            rbMidRight.background.setTint(midColors[2])
        }
    }
}

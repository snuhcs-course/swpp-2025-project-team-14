package com.example.mindlog.features.journal.presentation.write

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels // 1. activityViewModels 추가
import com.example.mindlog.R
import com.example.mindlog.databinding.FragmentEmotionSelectBinding
import com.example.mindlog.databinding.ItemEmotionRowBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EmotionSelectFragment : Fragment() {

    private var _binding: FragmentEmotionSelectBinding? = null
    private val binding get() = _binding!!

    // 2. Activity와 ViewModel을 공유
    private val viewModel: JournalWriteViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEmotionSelectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // API의 emotion 이름과 UI의 텍스트, 점수를 매핑
        setupEmotionRow(
            binding.rowSadHappy,
            "슬픔", "sad", "행복", "happy",
            R.color.emotion_sad_blue, R.color.emotion_happy_orange
        )
        setupEmotionRow(
            binding.rowAnxiousConfident,
            "불안", "anxious", "자신감", "confident",
            R.color.emotion_anxious_purple, R.color.emotion_confident_green
        )
        setupEmotionRow(
            binding.rowAngryCalm,
            "짜증/분노", "angry", "편안", "calm",
            R.color.emotion_angry_red, R.color.emotion_calm_green
        )
        setupEmotionRow(
            binding.rowBoredExcited,
            "지루함", "bored", "흥미/설렘", "excited",
            R.color.emotion_bored_blue, R.color.emotion_excited_pink
        )
        setupEmotionRow(
            binding.rowLethargicEnergetic,
            "무기력", "lethargic", "활력", "energetic",
            R.color.emotion_lethargic_indigo, R.color.emotion_happy_orange
        )
    }

    private fun setupEmotionRow(
        rowBinding: ItemEmotionRowBinding,
        leftText: String, leftApiName: String,
        rightText: String, rightApiName: String,
        leftColorRes: Int, rightColorRes: Int
    ) {
        val leftColor = ContextCompat.getColor(requireContext(), leftColorRes)
        val rightColor = ContextCompat.getColor(requireContext(), rightColorRes)
        val centerGrayColor = ContextCompat.getColor(requireContext(), R.color.emotion_lethargic_gray)

        rowBinding.tvEmotionLeft.text = leftText
        rowBinding.tvEmotionLeft.setTextColor(leftColor)
        rowBinding.tvEmotionRight.text = rightText
        rowBinding.tvEmotionRight.setTextColor(rightColor)

        // 색상 설정
        rowBinding.rbLeft.backgroundTintList = ColorStateList.valueOf(leftColor)
        rowBinding.rbCenter.backgroundTintList = ColorStateList.valueOf(centerGrayColor)
        rowBinding.rbRight.backgroundTintList = ColorStateList.valueOf(rightColor)
        rowBinding.rbMidLeft.backgroundTintList = ColorStateList.valueOf(interpolateColor(leftColor, centerGrayColor, 0.5f))
        rowBinding.rbMidRight.backgroundTintList = ColorStateList.valueOf(interpolateColor(centerGrayColor, rightColor, 0.5f))

        // 3. 라디오 그룹에 리스너 설정하여 ViewModel 업데이트
        rowBinding.radioGroupEmotion.setOnCheckedChangeListener { _, checkedId ->
            val scoreLeft = when (checkedId) {
                R.id.rb_left -> 2
                R.id.rb_mid_left -> 1
                else -> 0
            }
            val scoreRight = when (checkedId) {
                R.id.rb_right -> 2
                R.id.rb_mid_right -> 1
                else -> 0
            }
            // ViewModel의 함수를 호출하여 감정 점수 업데이트
            viewModel.updateEmotionScore(leftApiName, scoreLeft)
            viewModel.updateEmotionScore(rightApiName, scoreRight)
        }
    }


    private fun interpolateColor(color1: Int, color2: Int, ratio: Float): Int {
        val inverseRatio = 1 - ratio
        val a = (Color.alpha(color1) * inverseRatio + Color.alpha(color2) * ratio).toInt()
        val r = (Color.red(color1) * inverseRatio + Color.red(color2) * ratio).toInt()
        val g = (Color.green(color1) * inverseRatio + Color.green(color2) * ratio).toInt()
        val b = (Color.blue(color1) * inverseRatio + Color.blue(color2) * ratio).toInt()
        return Color.argb(a, r, g, b)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

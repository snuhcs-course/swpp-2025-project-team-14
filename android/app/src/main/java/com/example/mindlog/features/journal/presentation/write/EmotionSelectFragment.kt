package com.example.mindlog.features.journal.presentation.write

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.mindlog.R
import com.example.mindlog.databinding.FragmentEmotionSelectBinding
import com.example.mindlog.databinding.ItemEmotionRowBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EmotionSelectFragment : Fragment() {

    private var _binding: FragmentEmotionSelectBinding? = null
    private val binding get() = _binding!!

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

        observeAndSetupRow(binding.rowSadHappy, "슬픔", "sad", "행복", "happy", R.color.emotion_sad, R.color.emotion_happy)
        observeAndSetupRow(binding.rowAnxiousConfident, "불안", "anxious", "만족", "satisfied", R.color.emotion_anxious, R.color.emotion_satisfied)
        observeAndSetupRow(binding.rowAngryCalm, "짜증", "annoyed", "편안", "calm", R.color.emotion_annoyed, R.color.emotion_calm)
        observeAndSetupRow(binding.rowBoredExcited, "지루함", "bored", "흥미", "interested", R.color.emotion_bored, R.color.emotion_interested)
        observeAndSetupRow(binding.rowLethargicEnergetic, "무기력", "lethargic", "활력", "energetic", R.color.emotion_lethargic, R.color.emotion_energetic)
    }

    private fun observeAndSetupRow(
        rowBinding: ItemEmotionRowBinding,
        leftText: String, leftApiName: String,
        rightText: String, rightApiName: String,
        leftColorRes: Int, rightColorRes: Int
    ) {
        // --- 1. UI 겉모습 설정 ---
        val leftColor = ContextCompat.getColor(requireContext(), leftColorRes)
        val rightColor = ContextCompat.getColor(requireContext(), rightColorRes)
        val centerGrayColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
        rowBinding.tvEmotionLeft.text = leftText
        rowBinding.tvEmotionLeft.setTextColor(leftColor)
        rowBinding.tvEmotionRight.text = rightText
        rowBinding.tvEmotionRight.setTextColor(rightColor)

        rowBinding.rbLeft.backgroundTintList = ColorStateList.valueOf(leftColor)
        rowBinding.rbCenter.backgroundTintList = ColorStateList.valueOf(centerGrayColor)
        rowBinding.rbRight.backgroundTintList = ColorStateList.valueOf(rightColor)
        rowBinding.rbMidLeft.backgroundTintList = ColorStateList.valueOf(interpolateColor(leftColor, centerGrayColor, 0.5f))
        rowBinding.rbMidRight.backgroundTintList = ColorStateList.valueOf(interpolateColor(centerGrayColor, rightColor, 0.5f))

        // --- 2. 클릭 리스너 설정 (선택 취소/토글 로직) ---
        val radioButtons = listOf(
            rowBinding.rbLeft,
            rowBinding.rbMidLeft,
            rowBinding.rbCenter,
            rowBinding.rbMidRight,
            rowBinding.rbRight
        )

        radioButtons.forEach { rb ->
            rb.setOnClickListener {
                // 1) 클릭된 버튼이 의미하는 점수(0~4) 확인
                val clickedScore = when (rb.id) {
                    R.id.rb_left -> 4
                    R.id.rb_mid_left -> 3
                    R.id.rb_center -> 2
                    R.id.rb_mid_right -> 1
                    R.id.rb_right -> 0
                    else -> return@setOnClickListener
                }

                // 2) 현재 ViewModel에 저장된 '왼쪽 감정' 점수 확인
                val currentScore = viewModel.emotionScores.value[leftApiName]

                // 3) 이미 선택된 점수를 다시 눌렀다면 -> 선택 해제 (null 전송)
                if (currentScore == clickedScore) {
                    rowBinding.radioGroupEmotion.clearCheck()
                    viewModel.updateEmotionScore(leftApiName, null)
                    viewModel.updateEmotionScore(rightApiName, null)
                } else {
                    // 4) 새로운 점수 선택 -> ViewModel 업데이트
                    val rightScore = 4 - clickedScore
                    viewModel.updateEmotionScore(leftApiName, clickedScore)
                    viewModel.updateEmotionScore(rightApiName, rightScore)
                }
            }
        }

        // --- 3. 상태 관찰 및 UI 복원 ---
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.emotionScores
                    .map { it[leftApiName] }
                    .distinctUntilChanged()
                    .collect { scoreLeft ->
                        // ViewModel의 상태(scoreLeft)에 따라 UI 라디오 버튼 체크 상태 동기화

                        val buttonIdToCheck = when (scoreLeft) {
                            4 -> R.id.rb_left
                            3 -> R.id.rb_mid_left
                            2 -> R.id.rb_center
                            1 -> R.id.rb_mid_right
                            0 -> R.id.rb_right
                            else -> -1
                        }

                        if (buttonIdToCheck != -1) {
                            // 현재 UI 상태와 다를 때만 check 호출 (무한 루프 방지 및 리스너 충돌 방지)
                            if (rowBinding.radioGroupEmotion.checkedRadioButtonId != buttonIdToCheck) {
                                rowBinding.radioGroupEmotion.check(buttonIdToCheck)
                            }
                        } else {
                            // 값이 없으면(null) 선택 해제
                            rowBinding.radioGroupEmotion.clearCheck()
                        }
                    }
            }
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

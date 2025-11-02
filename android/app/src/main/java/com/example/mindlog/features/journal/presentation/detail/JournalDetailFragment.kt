package com.example.mindlog.features.journal.presentation.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.example.mindlog.BuildConfig
import com.example.mindlog.R
import com.example.mindlog.core.common.Result
import com.example.mindlog.core.model.Keyword
import com.example.mindlog.databinding.FragmentJournalDetailBinding
import com.example.mindlog.features.journal.presentation.write.JournalEditViewModel
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Locale

@AndroidEntryPoint
class JournalDetailFragment : Fragment() {

    private var _binding: FragmentJournalDetailBinding? = null
    private val binding get() = _binding!!

    // Activity와 ViewModel 공유 (데이터 로딩, 키워드 추출 등)
    private val viewModel: JournalEditViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJournalDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
    }

    private fun observeViewModel() {
        // 일기 데이터(제목, 내용 등) 관찰
        viewModel.journalState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Success -> {
                    val journal = result.data
                    binding.tvTitle.text = journal.title
                    binding.tvContent.text = journal.content
                    binding.tvGratitude.text = journal.gratitude

                    // 날짜 형식 변환
                    val serverDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    val displayFormat = SimpleDateFormat("yyyy년 MM월 dd일 E요일", Locale.KOREAN)
                    try {
                        val date = serverDateFormat.parse(journal.createdAt)
                        binding.tvDate.text = displayFormat.format(date!!)
                    } catch (e: Exception) {
                        binding.tvDate.text = "날짜 형식 오류"
                    }

                    // 이미지 표시
                    if (!journal.imageS3Keys.isNullOrBlank()) {
                        binding.ivImage.isVisible = true
                        val imageUrl = "${BuildConfig.S3_BUCKET_URL}/${journal.imageS3Keys}"
                        Glide.with(this).load(imageUrl).into(binding.ivImage)
                    } else {
                        binding.ivImage.isVisible = false
                    }
                }
                is Result.Error -> {
                    Toast.makeText(context, "데이터를 불러오는 데 실패했습니다: ${result.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        // 키워드 데이터 관찰
        viewModel.keywords.observe(viewLifecycleOwner) { keywords ->
            updateKeywordsUI(keywords)
        }
    }

    private fun updateKeywordsUI(keywords: List<Keyword>) {
        val hasKeywords = keywords.isNotEmpty()
        binding.labelKeywords.isVisible = hasKeywords
        binding.flexboxKeywords.isVisible = hasKeywords
        binding.flexboxKeywords.removeAllViews()

        if (hasKeywords) {
            keywords.forEach { keyword -> // 변수명을 keyword로 변경
                val keywordChip = createKeywordChip(keyword) // 변경된 변수 전달
                binding.flexboxKeywords.addView(keywordChip)
            }
        }
    }

    private fun createKeywordChip(keyword: Keyword): View {
        val keywordView = layoutInflater.inflate(R.layout.item_keyword_chip, binding.flexboxKeywords, false) as android.widget.TextView
        keywordView.text = "#${keyword.keyword}" // UI 모델의 필드 사용
        keywordView.setOnClickListener {
            // TODO: 추후 키워드 검색 기능 구현
            Toast.makeText(requireContext(), "'${keyword.keyword}' 키워드로 검색합니다.", Toast.LENGTH_SHORT).show()
        }
        return keywordView
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

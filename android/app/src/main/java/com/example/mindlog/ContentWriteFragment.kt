package com.example.mindlog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ContentWriteFragment : Fragment() {

    private lateinit var tvDate: TextView
    private lateinit var etTitle: EditText
    private lateinit var etContent: EditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_content_write, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvDate = view.findViewById(R.id.tv_date)
        etTitle = view.findViewById(R.id.et_title)
        etContent = view.findViewById(R.id.et_content)

        setCurrentDate()

        // TODO: 이미지 추가(layout_add_image) 및 하단 버튼(btn_add_photo, btn_generate_ai_photo)에 대한
        //  클릭 리스너나 로직을 여기에 추가할 수 있습니다.
    }

    private fun setCurrentDate() {
        val currentDate = Date()
        val format = SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault())
        tvDate.text = format.format(currentDate)
    }
}

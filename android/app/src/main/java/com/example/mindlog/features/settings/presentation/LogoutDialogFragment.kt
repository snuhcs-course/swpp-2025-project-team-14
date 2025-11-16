package com.example.mindlog.features.settings.presentation

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import androidx.fragment.app.DialogFragment
import com.example.mindlog.databinding.DialogLogoutBinding

class LogoutDialogFragment : DialogFragment() {
    private var _binding: DialogLogoutBinding? = null
    private val binding get() = _binding!!

    interface ConfirmDialogListener {
        fun onConfirm()
    }
    private var listener: ConfirmDialogListener? = null

    fun setConfirmDialogListener(listener: ConfirmDialogListener) {
        this.listener = listener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogLogoutBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return binding.root
    }

    // ✨ [핵심 수정] onStart()에서 다이얼로그의 크기와 위치를 직접 제어합니다.
    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            // 1. 너비를 화면에 꽉 채우고, 높이는 내용물에 맞게 설정합니다.
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)

            // 2. 다이얼로그 창 자체에 좌우 패딩을 줍니다. (32dp)
            val padding = (32 * resources.displayMetrics.density).toInt()
            decorView.setPadding(padding, 0, padding, 0)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnDialogCancel.setOnClickListener {
            dismiss()
        }

        binding.btnDialogConfirm.setOnClickListener {
            listener?.onConfirm()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): LogoutDialogFragment {
            return LogoutDialogFragment()
        }
    }
}

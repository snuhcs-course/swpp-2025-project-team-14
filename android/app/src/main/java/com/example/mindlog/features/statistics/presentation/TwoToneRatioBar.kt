package com.example.mindlog.features.statistics.widget

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.max
import kotlin.math.min

// TwoToneRatioBar.kt
class TwoToneRatioBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var leftPercent: Float = 40f
        set(v) { field = v.coerceIn(0f, 100f); invalidate() }
    var rightPercent: Float = 60f
        set(v) { field = v.coerceIn(0f, 100f); invalidate() }

    var leftColor: Int = Color.parseColor("#4D9BFF")
        set(v) { field = v; invalidate() }
    var rightColor: Int = Color.parseColor("#FFCD3C")
        set(v) { field = v; invalidate() }
    var trackColor: Int = Color.parseColor("#F1EDE4")
        set(v) { field = v; invalidate() }

    private val paint = Paint().apply {
        // 직사각형은 굳이 안티앨리어싱 없어도 경계가 더 또렷함
        isAntiAlias = false
        style = Paint.Style.FILL
    }
    private val rect = RectF()

    private fun dp(v: Float) = v * resources.displayMetrics.density

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val h = resolveSize(dp(14f).toInt(), heightMeasureSpec) // 막대 두께
        val w = resolveSize(suggestedMinimumWidth, widthMeasureSpec)
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1) 트랙(전체 바탕)
        paint.color = trackColor
        rect.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRect(rect, paint)

        // 2) 비율 계산 (합 0 방지)
        val lp = leftPercent.coerceIn(0f, 100f)
        val rp = rightPercent.coerceIn(0f, 100f)
        val sum = (lp + rp).coerceAtLeast(1f)
        val leftW = width * (lp / sum)
        val rightW = width * (rp / sum)

        // 3) 왼쪽 채움
        if (leftW >= 1f) {
            paint.color = leftColor
            rect.set(0f, 0f, leftW, height.toFloat())
            canvas.drawRect(rect, paint)
        }

        // 4) 오른쪽 채움
        if (rightW >= 1f) {
            paint.color = rightColor
            rect.set(width - rightW, 0f, width.toFloat(), height.toFloat())
            canvas.drawRect(rect, paint)
        }
    }
}
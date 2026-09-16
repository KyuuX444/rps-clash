package com.kyuu.rpsclash.ui.components

import android.app.Activity
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.kyuu.rpsclash.R

object CustomToast {

    fun show(activity: Activity, message: String, isError: Boolean = false) {
        activity.runOnUiThread {
            val root = activity.findViewById<ViewGroup>(android.R.id.content) ?: return@runOnUiThread

            val toastView = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                val bg = ContextCompat.getDrawable(activity, R.drawable.bg_card_glass)?.mutate()
                background = bg
                setPadding(36, 24, 36, 24)
                elevation = 16f

                val accentColor = if (isError) Color.parseColor("#FF1744") else Color.parseColor("#00E5FF")

                val textView = TextView(activity).apply {
                    text = message
                    setTextColor(Color.WHITE)
                    textSize = 14f
                    gravity = Gravity.CENTER
                }
                addView(textView)
            }

            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                topMargin = 96
            }

            root.addView(toastView, params)

            toastView.alpha = 0f
            toastView.translationY = -60f
            toastView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(250)
                .withEndAction {
                    toastView.postDelayed({
                        toastView.animate()
                            .alpha(0f)
                            .translationY(-40f)
                            .setDuration(250)
                            .withEndAction {
                                root.removeView(toastView)
                            }.start()
                    }, 2200)
                }.start()
        }
    }
}

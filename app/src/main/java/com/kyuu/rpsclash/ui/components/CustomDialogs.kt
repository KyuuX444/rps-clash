package com.kyuu.rpsclash.ui.components

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.kyuu.rpsclash.R
import com.kyuu.rpsclash.RPSApplication
import com.kyuu.rpsclash.nativebridge.NativeBridge

object CustomDialogs {

    fun showJoinRoomDialog(activity: Activity, onJoin: (String) -> Unit) {
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val layout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card_surface)
            setPadding(36, 32, 36, 32)
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            val title = TextView(activity).apply {
                text = "GABUNG ROOM"
                textSize = 17f
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
            }
            addView(title)

            val subtitle = TextView(activity).apply {
                text = "Masukkan 6 karakter kode room"
                textSize = 12f
                setTextColor(Color.parseColor("#94A3B8"))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 6 }
            }
            addView(subtitle)

            val input = EditText(activity).apply {
                hint = "Contoh: K9X2P1"
                setHintTextColor(Color.parseColor("#64748B"))
                setTextColor(Color.WHITE)
                textSize = 20f
                typeface = Typeface.DEFAULT_BOLD
                letterSpacing = 0.15f
                gravity = Gravity.CENTER
                inputType = InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
                filters = arrayOf(InputFilter.LengthFilter(6), InputFilter.AllCaps())

                val bg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(Color.parseColor("#0B0F17"))
                    cornerRadius = 14f
                    setStroke(2, Color.parseColor("#23334D"))
                }
                background = bg
                setPadding(24, 18, 24, 18)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 20
                    bottomMargin = 24
                }
            }
            addView(input)

            val btnRow = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                weightSum = 2f

                val btnCancel = Button(activity).apply {
                    text = "BATAL"
                    setTextColor(Color.parseColor("#94A3B8"))
                    setBackgroundResource(R.drawable.bg_pill)
                    layoutParams = LinearLayout.LayoutParams(0, 116, 1f).apply {
                        marginEnd = 12
                    }
                    setOnClickListener {
                        RPSApplication.instance.soundManager.playSfx(NativeBridge.SFX_TAP)
                        dialog.dismiss()
                    }
                }
                addView(btnCancel)

                val btnJoin = Button(activity).apply {
                    text = "GABUNG"
                    setTextColor(Color.BLACK)
                    typeface = Typeface.DEFAULT_BOLD
                    setBackgroundResource(R.drawable.bg_button_primary)
                    layoutParams = LinearLayout.LayoutParams(0, 116, 1f)
                    setOnClickListener {
                        val code = input.text.toString().trim()
                        if (code.length >= 4) {
                            RPSApplication.instance.soundManager.playSfx(NativeBridge.SFX_TAP)
                            dialog.dismiss()
                            onJoin(code)
                        } else {
                            CustomToast.show(activity, "Kode room minimal 4-6 karakter", true)
                        }
                    }
                }
                addView(btnJoin)
            }
            addView(btnRow)
        }

        dialog.setContentView(layout)
        dialog.show()
    }

    /**
     * Compact, elegant result modal supporting WIN, LOSE, and DRAW states
     * with subtle customized animations and exact metrics.
     */
    fun showMatchResultDialog(
        activity: Activity,
        resultState: Int, // NativeBridge.RESULT_WIN, RESULT_LOSE, RESULT_DRAW
        scoreText: String,
        totalRounds: Int,
        onRematch: () -> Unit,
        onExit: () -> Unit
    ): Dialog {
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val (accentColor, titleText, iconRes) = when (resultState) {
            NativeBridge.RESULT_WIN -> Triple(
                Color.parseColor("#10B981"),
                "VICTORY",
                R.drawable.ic_trophy
            )
            NativeBridge.RESULT_LOSE -> Triple(
                Color.parseColor("#EF4444"),
                "DEFEAT",
                R.drawable.ic_logo
            )
            else -> Triple(
                Color.parseColor("#F59E0B"),
                "DRAW MATCH",
                R.drawable.ic_vs
            )
        }

        val layout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(36, 32, 36, 32)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            val cardBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.parseColor("#162032"))
                cornerRadius = 18f
                setStroke(2, accentColor)
            }
            background = cardBg

            // Animated State Icon
            val icon = ImageView(activity).apply {
                layoutParams = LinearLayout.LayoutParams(64, 64).apply { bottomMargin = 14 }
                setImageDrawable(ContextCompat.getDrawable(activity, iconRes))
            }
            addView(icon)

            // Result Title
            val title = TextView(activity).apply {
                text = titleText
                textSize = 22f
                setTextColor(accentColor)
                typeface = Typeface.DEFAULT_BOLD
                letterSpacing = 0.08f
                gravity = Gravity.CENTER
            }
            addView(title)

            // Score Pill
            val scoreBadge = TextView(activity).apply {
                text = scoreText
                textSize = 24f
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setBackgroundResource(R.drawable.bg_pill)
                setPadding(32, 8, 32, 8)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 12
                    bottomMargin = 8
                }
            }
            addView(scoreBadge)

            // Round Info
            val roundInfo = TextView(activity).apply {
                text = if (totalRounds > 0) "$totalRounds Ronde Dimainkan" else "Pertandingan Selesai"
                textSize = 12f
                setTextColor(Color.parseColor("#94A3B8"))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 24
                }
            }
            addView(roundInfo)

            // Rematch Button
            val btnRematch = Button(activity).apply {
                text = "REMATCH"
                setTextColor(Color.BLACK)
                typeface = Typeface.DEFAULT_BOLD
                setBackgroundResource(R.drawable.bg_button_primary)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    114
                ).apply { bottomMargin = 10 }
                setOnClickListener {
                    RPSApplication.instance.soundManager.playSfx(NativeBridge.SFX_TAP)
                    dialog.dismiss()
                    onRematch()
                }
            }
            addView(btnRematch)

            // Back to Home Button
            val btnExit = Button(activity).apply {
                text = "KEMBALI KE HOME"
                setTextColor(Color.parseColor("#94A3B8"))
                setBackgroundResource(R.drawable.bg_pill)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    114
                )
                setOnClickListener {
                    RPSApplication.instance.soundManager.playSfx(NativeBridge.SFX_TAP)
                    dialog.dismiss()
                    onExit()
                }
            }
            addView(btnExit)
        }

        // Subtle animation when dialog opens
        val scaleX = ObjectAnimator.ofFloat(layout, View.SCALE_X, 0.88f, 1.0f)
        val scaleY = ObjectAnimator.ofFloat(layout, View.SCALE_Y, 0.88f, 1.0f)
        val alpha = ObjectAnimator.ofFloat(layout, View.ALPHA, 0.0f, 1.0f)
        AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            duration = 240
            interpolator = if (resultState == NativeBridge.RESULT_WIN) OvershootInterpolator(1.2f) else DecelerateInterpolator()
            start()
        }

        dialog.setContentView(layout)
        dialog.show()
        return dialog
    }

    fun showErrorDialog(activity: Activity, title: String, message: String, onDismiss: (() -> Unit)? = null) {
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val layout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card_surface)
            setPadding(32, 28, 32, 28)
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            val titleView = TextView(activity).apply {
                text = title
                textSize = 16f
                setTextColor(Color.parseColor("#EF4444"))
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
            }
            addView(titleView)

            val msgView = TextView(activity).apply {
                text = message
                textSize = 13f
                setTextColor(Color.parseColor("#94A3B8"))
                gravity = Gravity.CENTER
                setLineSpacing(0f, 1.25f)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 10
                    bottomMargin = 20
                }
            }
            addView(msgView)

            val btnOk = Button(activity).apply {
                text = "MENGERTI"
                setTextColor(Color.BLACK)
                typeface = Typeface.DEFAULT_BOLD
                setBackgroundResource(R.drawable.bg_button_primary)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    110
                )
                setOnClickListener {
                    RPSApplication.instance.soundManager.playSfx(NativeBridge.SFX_TAP)
                    dialog.dismiss()
                    onDismiss?.invoke()
                }
            }
            addView(btnOk)
        }

        dialog.setContentView(layout)
        dialog.show()
    }
}

package com.kyuu.rpsclash.ui.components

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
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
            setBackgroundResource(R.drawable.bg_card_glass)
            setPadding(48, 48, 48, 48)
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

            val title = TextView(activity).apply {
                text = "MASUKKAN KODE ROOM"
                textSize = 18f
                setTextColor(Color.WHITE)
                isFakeBoldText = true
                gravity = Gravity.CENTER
            }
            addView(title)

            val input = EditText(activity).apply {
                hint = "Contoh: K9X2P1"
                setHintTextColor(Color.parseColor("#64748B"))
                setTextColor(Color.WHITE)
                textSize = 20f
                gravity = Gravity.CENTER
                inputType = InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
                filters = arrayOf(InputFilter.LengthFilter(6), InputFilter.AllCaps())
                setBackgroundResource(R.drawable.bg_card_surface)
                setPadding(32, 28, 32, 28)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 32
                    bottomMargin = 32
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
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        marginEnd = 16
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
                    isFakeBoldText = true
                    setBackgroundResource(R.drawable.bg_button_primary)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
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

    fun showMatchResultDialog(
        activity: Activity,
        won: Boolean,
        title: String,
        scoreText: String,
        onRematch: () -> Unit,
        onExit: () -> Unit
    ): Dialog {
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val layout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card_glass)
            setPadding(56, 56, 56, 56)
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

            val icon = ImageView(activity).apply {
                layoutParams = LinearLayout.LayoutParams(120, 120).apply { bottomMargin = 24 }
                val drawableRes = if (won) R.drawable.ic_trophy else R.drawable.ic_logo
                setImageDrawable(ContextCompat.getDrawable(activity, drawableRes))
            }
            addView(icon)

            val titleText = TextView(activity).apply {
                text = title
                textSize = 22f
                setTextColor(if (won) Color.parseColor("#00E676") else Color.parseColor("#FF1744"))
                isFakeBoldText = true
                gravity = Gravity.CENTER
            }
            addView(titleText)

            val scoreDisplay = TextView(activity).apply {
                text = "SKOR AKHIR: $scoreText"
                textSize = 16f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 16
                    bottomMargin = 36
                }
            }
            addView(scoreDisplay)

            val btnRematch = Button(activity).apply {
                text = "REMATCH"
                setTextColor(Color.BLACK)
                isFakeBoldText = true
                setBackgroundResource(R.drawable.bg_button_primary)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 16 }
                setOnClickListener {
                    RPSApplication.instance.soundManager.playSfx(NativeBridge.SFX_TAP)
                    dialog.dismiss()
                    onRematch()
                }
            }
            addView(btnRematch)

            val btnExit = Button(activity).apply {
                text = "KELUAR KE MENU"
                setTextColor(Color.WHITE)
                setBackgroundResource(R.drawable.bg_button_danger)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setOnClickListener {
                    RPSApplication.instance.soundManager.playSfx(NativeBridge.SFX_TAP)
                    dialog.dismiss()
                    onExit()
                }
            }
            addView(btnExit)
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
            setBackgroundResource(R.drawable.bg_card_glass)
            setPadding(48, 48, 48, 48)
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

            val titleView = TextView(activity).apply {
                text = title
                textSize = 18f
                setTextColor(Color.parseColor("#FF1744"))
                isFakeBoldText = true
                gravity = Gravity.CENTER
            }
            addView(titleView)

            val msgView = TextView(activity).apply {
                text = message
                textSize = 14f
                setTextColor(Color.parseColor("#CBD5E1"))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 20
                    bottomMargin = 32
                }
            }
            addView(msgView)

            val btnOk = Button(activity).apply {
                text = "MENGERTI"
                setTextColor(Color.BLACK)
                isFakeBoldText = true
                setBackgroundResource(R.drawable.bg_button_primary)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
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

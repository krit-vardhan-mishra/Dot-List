package com.just_for_fun.dotlist.utils

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color.WHITE
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import com.just_for_fun.dotlist.R

object ToastUtil {

    fun showCustomToast(context: Context, message: String) {
        val dialog = Dialog(context).apply {
            setCancelable(false)
            setCanceledOnTouchOutside(false)
        }

        val view: View = LayoutInflater.from(context).inflate(R.layout.custom_toast_layout, null)
        val textView: TextView = view.findViewById(R.id.toast_message)
        textView.text = message
        textView.setTextColor(WHITE)
        dialog.setContentView(view)

        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)

            val params = attributes?.apply {
                gravity = Gravity.BOTTOM
                y = 150
                dimAmount = 0f
                flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            }
            attributes = params

        }

        dialog.show()

        Handler(Looper.getMainLooper()).postDelayed({
            dialog.dismiss()
        }, 2000L)
    }

    fun showCustomToast(context: Context, message: String, durationInSecs: Int) {
        val dialog = Dialog(context).apply {
            setCancelable(false)
            setCanceledOnTouchOutside(false)
        }

        val view: View = LayoutInflater.from(context).inflate(R.layout.custom_toast_layout, null)
        val textView: TextView = view.findViewById(R.id.toast_message)
        textView.text = message
        textView.setTextColor(WHITE)
        dialog.setContentView(view)

        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)

            val params = attributes?.apply {
                gravity = Gravity.BOTTOM
                y = 150
                dimAmount = 0f
                flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            }
            attributes = params

        }

        dialog.show()

        Handler(Looper.getMainLooper()).postDelayed({
            dialog.dismiss()
        }, durationInSecs * 1000L)
    }

    fun showCustomToast(context: Context, message: String, durationInSecs: Int, color: Int) {
        val dialog = Dialog(context).apply {
            setCancelable(false)
            setCanceledOnTouchOutside(false)
        }

        val view: View = LayoutInflater.from(context).inflate(R.layout.custom_toast_layout, null)
        val textView: TextView = view.findViewById(R.id.toast_message)
        textView.text = message
        textView.setTextColor(WHITE)
        dialog.setContentView(view)

        val lighterColor = ColorUtils.blendARGB(color, WHITE, 0.5f)
        view.backgroundTintList = ColorStateList.valueOf(lighterColor)
        dialog.setContentView(view)

        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)

            val params = attributes?.apply {
                gravity = Gravity.BOTTOM
                y = 150
                dimAmount = 0f
                flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            }
            attributes = params

        }

        dialog.show()

        Handler(Looper.getMainLooper()).postDelayed({
            dialog.dismiss()
        }, durationInSecs * 1000L)
    }
}
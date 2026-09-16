package com.kyuu.rpsclash.ui.components

import android.widget.TextView

var TextView.isFakeBoldText: Boolean
    get() = paint.isFakeBoldText
    set(value) {
        paint.isFakeBoldText = value
    }

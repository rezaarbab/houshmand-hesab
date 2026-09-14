package com.houshmandhesab.app.util

import androidx.compose.ui.graphics.Color

fun toColor(value: Long): Color = Color(value.toULong().toInt() or 0xFF000000u.toInt())

fun toColorArgb(value: Long): Color = Color(value)

package com.checkrate.app.ui

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun appOutlinedTextFieldColors(): TextFieldColors {
    val container = Color(0xFFE8F4FD)
    val border = Color(0xFFBBDEFB)
    val text = Color(0xFF000000)
    val placeholder = Color(0xFF717182)

    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = container,
        unfocusedContainerColor = container,
        disabledContainerColor = container,
        focusedBorderColor = border,
        unfocusedBorderColor = border,
        disabledBorderColor = border,
        focusedTextColor = text,
        unfocusedTextColor = text,
        disabledTextColor = text,
        focusedPlaceholderColor = placeholder,
        unfocusedPlaceholderColor = placeholder,
        cursorColor = text,
        focusedLabelColor = text,
        unfocusedLabelColor = placeholder
    )
}

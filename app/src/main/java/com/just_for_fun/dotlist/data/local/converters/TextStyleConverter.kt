package com.just_for_fun.dotlist.data.local.converters

import androidx.room.TypeConverter
import com.just_for_fun.dotlist.domain.models.TextStyle

class TextStyleConverter {
    @TypeConverter
    fun fromTextStyle(style: TextStyle): String {
        return style.name
    }

    @TypeConverter
    fun toTextStyle(styleName: String): TextStyle {
        return TextStyle.valueOf(styleName)
    }
}
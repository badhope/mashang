package com.pyneon.academy.utils

import android.content.Context
import android.content.SharedPreferences
import com.pyneon.academy.ui.theme.AppTheme
import com.pyneon.academy.ui.theme.AppThemes
import com.pyneon.academy.ui.theme.FontScale
import com.pyneon.academy.ui.theme.VisualStyle

// 主题偏好都存在 SharedPreferences。配色和风格分开存，可以随便组合。
object ThemePreference {
    private const val PREFS_NAME = "pyneon_theme"
    const val KEY_THEME_ID = "theme_id"
    const val KEY_CUSTOM_COLOR = "custom_color"
    const val KEY_VISUAL_STYLE = "visual_style"
    const val KEY_FONT_SCALE = "font_scale"

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // 主题一变就通知，MainActivity 拿来做即时换肤。
    fun registerListener(context: Context, listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        getPrefs(context).registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(context: Context, listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        getPrefs(context).unregisterOnSharedPreferenceChangeListener(listener)
    }

    // 自定义主题按用户给的主色现算，其余按 id 查表。
    fun getCurrentTheme(context: Context): AppTheme {
        val themeId = getCurrentThemeId(context)
        return if (themeId == AppThemes.CUSTOM_THEME_ID) {
            AppThemes.customTheme(getCustomColor(context))
        } else {
            AppThemes.getThemeById(themeId)
        }
    }


    fun saveTheme(context: Context, themeId: String) {
        getPrefs(context).edit().putString(KEY_THEME_ID, themeId).apply()
    }

    // 当前主题 id。默认白纸模式，看长文舒服；换过的用户保持自己的选择。

    fun getCurrentThemeId(context: Context): String {
        return getPrefs(context).getString(KEY_THEME_ID, DEFAULT_THEME_ID) ?: DEFAULT_THEME_ID
    }

    // 首次安装用白纸模式。
    const val DEFAULT_THEME_ID = "paper_light"


    fun getCustomColor(context: Context): String {
        return getPrefs(context).getString(KEY_CUSTOM_COLOR, "#00E5FF") ?: "#00E5FF"
    }


    fun saveCustomColor(context: Context, hex: String) {
        getPrefs(context).edit().putString(KEY_CUSTOM_COLOR, hex).apply()
    }

    // 视觉风格，和配色分开。

    // 没设置过就用默认赛博朋克。
    fun getVisualStyle(context: Context): VisualStyle =
        VisualStyle.fromId(getPrefs(context).getString(KEY_VISUAL_STYLE, null))


    fun saveVisualStyle(context: Context, style: VisualStyle) {
        getPrefs(context).edit().putString(KEY_VISUAL_STYLE, style.id).apply()
    }

    // 字号缩放。

    // 没设置过就用标准档。
    fun getFontScale(context: Context): FontScale =
        FontScale.fromId(getPrefs(context).getString(KEY_FONT_SCALE, null))

    fun saveFontScale(context: Context, scale: FontScale) {
        getPrefs(context).edit().putString(KEY_FONT_SCALE, scale.id).apply()
    }
}

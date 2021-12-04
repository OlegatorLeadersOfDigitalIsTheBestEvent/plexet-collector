package team.plexet.product.settings

import android.content.Context

private const val APP_PREFERENCES = "APP_PREFERENCES"
private const val PERIOD = "PERIOD"
private const val TRACK_NUM = "TRACK_NUM"
private const val NAME = "NAME"

fun setPeriod(context: Context, value: Long){
    val settings = context.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
    val editor = settings.edit()
    editor.putLong(PERIOD, value)
    editor.commit()
}

fun getPeriod(context: Context): Long{
    val settings = context.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
    return settings.getLong(PERIOD, 1000)
}

fun setTrackNum(context: Context, value: Long){
    val settings = context.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
    val editor = settings.edit()
    editor.putLong(TRACK_NUM, value)
    editor.commit()
}

fun getTrackNum(context: Context): Long{
    val settings = context.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
    return settings.getLong(TRACK_NUM, 1)
}

fun setName(context: Context, value: String){
    val settings = context.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
    val editor = settings.edit()
    editor.putString(NAME, value)
    editor.commit()
}

fun getName(context: Context): String? {
    val settings = context.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
    return settings.getString(NAME, "child_name")
}
// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.testutil

import android.content.SharedPreferences

/** In-memory [SharedPreferences] for JVM unit tests; edits apply at once. */
class FakePrefs(val data: MutableMap<String, Any?> = mutableMapOf()) : SharedPreferences {
    override fun getAll(): MutableMap<String, *> = data.toMutableMap()
    override fun getString(key: String, defValue: String?) = data[key] as String? ?: defValue
    override fun getStringSet(key: String, defValues: MutableSet<String>?) = defValues
    override fun getInt(key: String, defValue: Int) = data[key] as Int? ?: defValue
    override fun getLong(key: String, defValue: Long) = data[key] as Long? ?: defValue
    override fun getFloat(key: String, defValue: Float) = data[key] as Float? ?: defValue
    override fun getBoolean(key: String, defValue: Boolean) = data[key] as Boolean? ?: defValue
    override fun contains(key: String) = data.containsKey(key)
    override fun registerOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
        override fun putString(key: String, value: String?) = apply { data[key] = value }
        override fun putStringSet(key: String, values: MutableSet<String>?) = apply { data[key] = values }
        override fun putInt(key: String, value: Int) = apply { data[key] = value }
        override fun putLong(key: String, value: Long) = apply { data[key] = value }
        override fun putFloat(key: String, value: Float) = apply { data[key] = value }
        override fun putBoolean(key: String, value: Boolean) = apply { data[key] = value }
        override fun remove(key: String) = apply { data.remove(key) }
        override fun clear() = apply { data.clear() }
        override fun commit() = true
        override fun apply() {}
    }
}

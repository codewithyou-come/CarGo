package com.example.game

import android.content.Context
import android.content.SharedPreferences

class GamePreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("turbo_racer_prefs", Context.MODE_PRIVATE)

    var highScore: Int
        get() = prefs.getInt("high_score", 0)
        set(value) = prefs.edit().putInt("high_score", value).apply()

    var totalCoins: Int
        get() = prefs.getInt("total_coins", 0)
        set(value) = prefs.edit().putInt("total_coins", value).apply()

    var soundEnabled: Boolean
        get() = prefs.getBoolean("sound_enabled", true)
        set(value) = prefs.edit().putBoolean("sound_enabled", value).apply()

    var hapticsEnabled: Boolean
        get() = prefs.getBoolean("haptics_enabled", true)
        set(value) = prefs.edit().putBoolean("haptics_enabled", value).apply()

    var controlMode: ControlMode
        get() {
            val name = prefs.getString("control_mode", ControlMode.TOUCH_DRAG.name)
            return try {
                ControlMode.valueOf(name ?: ControlMode.TOUCH_DRAG.name)
            } catch (_: Exception) {
                ControlMode.TOUCH_DRAG
            }
        }
        set(value) = prefs.edit().putString("control_mode", value.name).apply()

    var selectedCarId: String
        get() = prefs.getString("selected_car_id", "red_comet") ?: "red_comet"
        set(value) = prefs.edit().putString("selected_car_id", value).apply()

    fun isCarUnlocked(carId: String): Boolean {
        if (carId == "red_comet") return true
        val unlocked = prefs.getStringSet("unlocked_cars", emptySet()) ?: emptySet()
        return unlocked.contains(carId)
    }

    fun unlockCar(carId: String, cost: Int): Boolean {
        if (totalCoins < cost) return false
        totalCoins -= cost
        val unlocked = prefs.getStringSet("unlocked_cars", emptySet())?.toMutableSet() ?: mutableSetOf()
        unlocked.add(carId)
        prefs.edit().putStringSet("unlocked_cars", unlocked).apply()
        return true
    }
}

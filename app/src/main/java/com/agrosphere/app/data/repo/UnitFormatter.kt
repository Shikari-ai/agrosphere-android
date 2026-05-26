package com.agrosphere.app.data.repo

object UnitFormatter {
    fun temp(celsius: Int, metric: Boolean): String =
        if (metric) "$celsius°C" else "${toF(celsius)}°F"

    fun tempShort(celsius: Int, metric: Boolean): String =
        if (metric) "$celsius°" else "${toF(celsius)}°"

    fun feelsLike(celsius: Int, metric: Boolean): String =
        if (metric) "Feels like $celsius°C" else "Feels like ${toF(celsius)}°F"

    fun wind(kph: Int, metric: Boolean): String =
        if (metric) "$kph km/h" else "${(kph * 0.621371).toInt()} mph"

    fun rain(mm: Float, metric: Boolean): String =
        if (metric) "${"%.1f".format(mm)} mm" else "${"%.2f".format(mm / 25.4f)} in"

    fun area(ha: Double, metric: Boolean): String =
        if (metric) "${"%.2f".format(ha)} ha" else "${"%.2f".format(ha * 2.47105)} ac"

    private fun toF(c: Int): Int = (c * 9.0 / 5.0 + 32).toInt()
}

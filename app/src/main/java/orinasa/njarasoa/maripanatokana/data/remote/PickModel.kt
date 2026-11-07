// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.data.remote

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private const val BEST_MATCH = "best_match"

/**
 * The forecast with [model] (a key of WEATHER_MODELS), or with the Open-Meteo choice when [model] is null.
 * The main app and the widgets use this function, so they make the same request.
 */
suspend fun OpenMeteoApiService.getForecastFor(latitude: Double, longitude: Double, model: String?, json: Json): OpenMeteoResponse {
    if (model == null) return getForecast(latitude = latitude, longitude = longitude)
    val raw = getForecastWithModels(latitude = latitude, longitude = longitude, models = "$model,$BEST_MATCH")
    return json.decodeFromJsonElement(OpenMeteoResponse.serializer(), pickModel(raw, model))
}

/**
 * Merges a response for "[model],best_match" into the shape of a response for one model.
 * Hourly, daily and minutely values: the [model] value when it is not null, else the best_match value.
 * The result has as many entries as best_match, so the days after the [model] horizon come from best_match.
 * Current values (not suffixed, from [model]): a null value takes the best_match hourly value of the current hour.
 */
fun pickModel(response: JsonObject, model: String): JsonObject {
    val hourly = response["hourly"] as? JsonObject
    val merged = response.toMutableMap()
    for (section in listOf("hourly", "daily", "minutely_15")) {
        (response[section] as? JsonObject)?.let { merged[section] = mergeSection(it, model) }
    }
    (response["current"] as? JsonObject)?.let { current ->
        val hour = (current["time"] as? JsonPrimitive)?.content?.take(13)?.let { "$it:00" }
        val index = (hourly?.get("time") as? JsonArray)?.indexOfFirst { (it as? JsonPrimitive)?.content == hour } ?: -1
        merged["current"] = JsonObject(current.mapValues { (key, value) ->
            if (value !is JsonNull || index < 0) value
            else (hourly?.get("${key}_$BEST_MATCH") as? JsonArray)?.getOrNull(index) ?: JsonNull
        })
    }
    return JsonObject(merged)
}

/** One section (hourly, daily or minutely_15) with the model suffixes removed. Keys without a suffix stay. */
private fun mergeSection(section: JsonObject, model: String): JsonObject {
    val chosenSuffix = "_$model"
    val fallbackSuffix = "_$BEST_MATCH"
    val out = LinkedHashMap<String, JsonElement>()
    for ((key, value) in section) {
        when {
            key.endsWith(fallbackSuffix) -> {
                val base = key.removeSuffix(fallbackSuffix)
                out[base] = mergeValues(section[base + chosenSuffix], value)
            }
            key.endsWith(chosenSuffix) -> {
                val base = key.removeSuffix(chosenSuffix)
                if (section[base + fallbackSuffix] == null) out[base] = value
            }
            else -> out.putIfAbsent(key, value)
        }
    }
    return JsonObject(out)
}

private fun mergeValues(chosen: JsonElement?, fallback: JsonElement): JsonElement {
    if (fallback !is JsonArray) return chosen?.takeIf { it !is JsonNull } ?: fallback
    val chosenValues = chosen as? JsonArray
    return JsonArray(fallback.mapIndexed { i, value ->
        chosenValues?.getOrNull(i)?.takeIf { it !is JsonNull } ?: value
    })
}

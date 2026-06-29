package orinasa.njarasoa.maripanatokana.data.remote

object JmaAreaCodes {
    fun isInJapan(lat: Double, lon: Double) = lat in 24.0..46.0 && lon in 122.0..154.0

    fun nearestPrefectureCode(lat: Double, lon: Double): String {
        return PREFECTURES.minByOrNull { (_, cLat, cLon) ->
            val dLat = lat - cLat; val dLon = lon - cLon
            dLat * dLat + dLon * dLon
        }?.first ?: "130000"
    }

    fun prefectureName(areaCode: String): String? {
        val prefix = areaCode.take(2)
        return PREFECTURES.firstOrNull { it.first.take(2) == prefix }
            ?.let { PREFECTURE_NAMES[it.first] }
    }

    private val PREFECTURE_NAMES = mapOf(
        "016000" to "Hokkaido", "020000" to "Aomori", "030000" to "Iwate",
        "040000" to "Miyagi", "050000" to "Akita", "060000" to "Yamagata",
        "070000" to "Fukushima", "080000" to "Ibaraki", "090000" to "Tochigi",
        "100000" to "Gunma", "110000" to "Saitama", "120000" to "Chiba",
        "130000" to "Tokyo", "140000" to "Kanagawa", "150000" to "Niigata",
        "160000" to "Toyama", "170000" to "Ishikawa", "180000" to "Fukui",
        "190000" to "Yamanashi", "200000" to "Nagano", "210000" to "Gifu",
        "220000" to "Shizuoka", "230000" to "Aichi", "240000" to "Mie",
        "250000" to "Shiga", "260000" to "Kyoto", "270000" to "Osaka",
        "280000" to "Hyogo", "290000" to "Nara", "300000" to "Wakayama",
        "310000" to "Tottori", "320000" to "Shimane", "330000" to "Okayama",
        "340000" to "Hiroshima", "350000" to "Yamaguchi", "360000" to "Tokushima",
        "370000" to "Kagawa", "380000" to "Ehime", "390000" to "Kochi",
        "400000" to "Fukuoka", "410000" to "Saga", "420000" to "Nagasaki",
        "430000" to "Kumamoto", "440000" to "Oita", "450000" to "Miyazaki",
        "460000" to "Kagoshima", "471000" to "Okinawa"
    )

    // (code, center_lat, center_lon) for all 47 prefectures
    private val PREFECTURES = arrayOf(
        Triple("016000", 43.06, 141.35), // Hokkaido
        Triple("020000", 40.82, 140.74), // Aomori
        Triple("030000", 39.70, 141.15), // Iwate
        Triple("040000", 38.27, 140.87), // Miyagi
        Triple("050000", 39.72, 140.10), // Akita
        Triple("060000", 38.24, 140.36), // Yamagata
        Triple("070000", 37.75, 140.47), // Fukushima
        Triple("080000", 36.34, 140.45), // Ibaraki
        Triple("090000", 36.57, 139.88), // Tochigi
        Triple("100000", 36.39, 139.06), // Gunma
        Triple("110000", 35.86, 139.65), // Saitama
        Triple("120000", 35.61, 140.12), // Chiba
        Triple("130000", 35.69, 139.69), // Tokyo
        Triple("140000", 35.45, 139.64), // Kanagawa
        Triple("150000", 37.90, 139.02), // Niigata
        Triple("160000", 36.70, 137.21), // Toyama
        Triple("170000", 36.59, 136.63), // Ishikawa
        Triple("180000", 36.07, 136.22), // Fukui
        Triple("190000", 35.66, 138.57), // Yamanashi
        Triple("200000", 36.65, 138.18), // Nagano
        Triple("210000", 35.39, 136.72), // Gifu
        Triple("220000", 34.98, 138.38), // Shizuoka
        Triple("230000", 35.18, 136.91), // Aichi
        Triple("240000", 34.73, 136.51), // Mie
        Triple("250000", 35.00, 135.87), // Shiga
        Triple("260000", 35.02, 135.76), // Kyoto
        Triple("270000", 34.69, 135.50), // Osaka
        Triple("280000", 34.69, 135.18), // Hyogo
        Triple("290000", 34.69, 135.83), // Nara
        Triple("300000", 34.23, 135.17), // Wakayama
        Triple("310000", 35.50, 134.24), // Tottori
        Triple("320000", 35.47, 133.05), // Shimane
        Triple("330000", 34.66, 133.93), // Okayama
        Triple("340000", 34.40, 132.46), // Hiroshima
        Triple("350000", 34.19, 131.47), // Yamaguchi
        Triple("360000", 34.07, 134.55), // Tokushima
        Triple("370000", 34.34, 134.04), // Kagawa
        Triple("380000", 33.84, 132.77), // Ehime
        Triple("390000", 33.56, 133.53), // Kochi
        Triple("400000", 33.61, 130.42), // Fukuoka
        Triple("410000", 33.25, 130.30), // Saga
        Triple("420000", 32.74, 129.87), // Nagasaki
        Triple("430000", 32.79, 130.74), // Kumamoto
        Triple("440000", 33.24, 131.61), // Oita
        Triple("450000", 31.91, 131.42), // Miyazaki
        Triple("460000", 31.56, 130.56), // Kagoshima
        Triple("471000", 26.21, 127.68), // Okinawa
    )
}

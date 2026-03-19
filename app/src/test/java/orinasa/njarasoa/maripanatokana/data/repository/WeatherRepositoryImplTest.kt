package orinasa.njarasoa.maripanatokana.data.repository

import kotlinx.coroutines.runBlocking
import org.junit.Test
import orinasa.njarasoa.maripanatokana.domain.model.AlertLevel
import orinasa.njarasoa.maripanatokana.domain.model.WeatherAlert
import kotlin.system.measureNanoTime

class WeatherRepositoryImplTest {

    @Test
    fun benchmarkDistinctBy() {
        // Reduced size since normally there are maybe 5-10 alerts
        val nwsAlerts = List(10) { WeatherAlert(AlertLevel.WARNING, "Title $it", "Desc $it", "nws", 0L, null, null) }
        val gdacsAlerts = List(10) { WeatherAlert(AlertLevel.WARNING, "Title $it", "Desc $it", "gdacs", 0L, null, null) }
        val weatherDataAlerts = List(10) { WeatherAlert(AlertLevel.WARNING, "Title $it", "Desc $it", "open-meteo", 0L, null, null) }

        // Warm up
        repeat(5000) {
            (nwsAlerts + gdacsAlerts + weatherDataAlerts).distinctBy { it.titleKey + it.source }
        }

        val baselineTime = measureNanoTime {
            repeat(10000) {
                (nwsAlerts + gdacsAlerts + weatherDataAlerts).distinctBy { it.titleKey + it.source }
            }
        }
        println("Baseline distinctBy time: ${baselineTime / 1_000_000.0} ms")

        // Build list with explicit sizes
        repeat(5000) {
            buildList(nwsAlerts.size + gdacsAlerts.size + weatherDataAlerts.size) {
                addAll(nwsAlerts)
                addAll(gdacsAlerts)
                addAll(weatherDataAlerts)
            }.distinctBy { it.titleKey + it.source }
        }
        val buildListTime = measureNanoTime {
            repeat(10000) {
                buildList(nwsAlerts.size + gdacsAlerts.size + weatherDataAlerts.size) {
                    addAll(nwsAlerts)
                    addAll(gdacsAlerts)
                    addAll(weatherDataAlerts)
                }.distinctBy { it.titleKey + it.source }
            }
        }
        println("BuildList distinctBy time: ${buildListTime / 1_000_000.0} ms")

        // Build list avoiding distinctBy allocations
        repeat(5000) {
            val capacity = nwsAlerts.size + gdacsAlerts.size + weatherDataAlerts.size
            val seen = HashSet<String>(capacity)
            val combined = ArrayList<WeatherAlert>(capacity)

            fun addIfNew(list: List<WeatherAlert>) {
                for (i in list.indices) {
                    val alert = list[i]
                    if (seen.add(alert.titleKey + alert.source)) {
                        combined.add(alert)
                    }
                }
            }

            addIfNew(nwsAlerts)
            addIfNew(gdacsAlerts)
            addIfNew(weatherDataAlerts)
        }
        val addIfNewTime = measureNanoTime {
            repeat(10000) {
                val capacity = nwsAlerts.size + gdacsAlerts.size + weatherDataAlerts.size
                val seen = HashSet<String>(capacity)
                val combined = ArrayList<WeatherAlert>(capacity)

                fun addIfNew(list: List<WeatherAlert>) {
                    for (i in list.indices) {
                        val alert = list[i]
                        if (seen.add(alert.titleKey + alert.source)) {
                            combined.add(alert)
                        }
                    }
                }

                addIfNew(nwsAlerts)
                addIfNew(gdacsAlerts)
                addIfNew(weatherDataAlerts)
            }
        }
        println("AddIfNew time: ${addIfNewTime / 1_000_000.0} ms")
    }
}

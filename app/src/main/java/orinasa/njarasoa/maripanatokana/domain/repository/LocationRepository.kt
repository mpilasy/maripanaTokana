// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.domain.repository

interface LocationRepository {
    suspend fun getLastLocation(): Result<Pair<Double, Double>>
    suspend fun getFreshLocation(): Result<Pair<Double, Double>>
}

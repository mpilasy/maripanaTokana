// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.domain.repository

import orinasa.njarasoa.maripanatokana.domain.model.LocationFix

interface LocationRepository {
    suspend fun getLastLocation(): Result<LocationFix>
    suspend fun getFreshLocation(): Result<LocationFix>
}

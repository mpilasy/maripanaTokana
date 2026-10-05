package orinasa.njarasoa.maripanatokana.domain.model

/** Why a weather fetch failed, so the UI can say something more useful than "failed". */
enum class FetchError { Offline, Timeout, RateLimited, Server, InvalidApiKey, Generic }

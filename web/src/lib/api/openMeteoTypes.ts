export interface OpenMeteoResponse {
	latitude: number;
	longitude: number;
	utc_offset_seconds: number;
	current: OpenMeteoCurrent;
	daily: OpenMeteoDaily;
	hourly: OpenMeteoHourly;
	minutely_15?: OpenMeteoMinutely15;
}

export interface OpenMeteoMinutely15 {
	time: string[];
	precipitation: (number | null)[];
}

export interface OpenMeteoCurrent {
	temperature_2m: number;
	apparent_temperature: number | null;
	relative_humidity_2m: number | null;
	dew_point_2m: number | null;
	wind_speed_10m: number | null;
	wind_direction_10m: number | null;
	wind_gusts_10m: number | null;
	pressure_msl: number | null;
	precipitation: number | null;
	rain: number | null;
	snowfall: number | null;
	visibility: number | null;
	weather_code: number;
	is_day: number;
	uv_index: number | null;
	cloud_cover: number | null;
}

export interface OpenMeteoDaily {
	time: string[];
	temperature_2m_max: number[];
	temperature_2m_min: number[];
	weather_code: number[];
	precipitation_probability_max: (number | null)[];
	sunrise: string[];
	sunset: string[];
	wind_speed_10m_max: (number | null)[];
	wind_direction_10m_dominant: (number | null)[];
	precipitation_sum: (number | null)[];
	uv_index_max: (number | null)[];
}

export interface OpenMeteoHourly {
	time: string[];
	temperature_2m: number[];
	weather_code: number[];
	precipitation_probability: (number | null)[];
	wind_speed_10m: (number | null)[];
	wind_direction_10m: (number | null)[];
	pressure_msl: (number | null)[];
	precipitation: (number | null)[];
}

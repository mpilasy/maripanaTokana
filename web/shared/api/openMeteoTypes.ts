export interface OpenMeteoResponse {
	current: OpenMeteoCurrent;
	daily: OpenMeteoDaily;
	hourly: OpenMeteoHourly;
}

export interface OpenMeteoCurrent {
	temperature_2m: number;
	apparent_temperature: number;
	relative_humidity_2m: number;
	dew_point_2m: number;
	wind_speed_10m: number;
	wind_direction_10m: number;
	wind_gusts_10m: number;
	pressure_msl: number;
	precipitation: number;
	rain: number;
	snowfall: number;
	visibility: number;
	weather_code: number;
	is_day: number;
	uv_index: number;
	cloud_cover: number;
}

export interface OpenMeteoDaily {
	time: string[];
	temperature_2m_max: number[];
	temperature_2m_min: number[];
	weather_code: number[];
	precipitation_probability_max: number[];
	sunrise: string[];
	sunset: string[];
}

export interface OpenMeteoHourly {
	time: string[];
	temperature_2m: number[];
	weather_code: number[];
	precipitation_probability: number[];
}

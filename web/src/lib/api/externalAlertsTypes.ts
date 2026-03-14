export interface WeatherApiAlertResponse {
	alerts?: {
		alert: {
			headline: string;
			severity: string;
			event: string;
			desc: string;
			instruction: string;
		}[];
	};
}

export interface GdacsAlertResponse {
	features: {
		properties: {
			name: string;
			eventtype: string;
			alertlevel: string;
			description: string;
			severity: string;
		};
		geometry: {
			coordinates: [number, number]; // [lon, lat]
		};
	}[];
}

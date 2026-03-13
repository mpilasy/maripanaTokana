export interface NwsAlertResponse {
	features: {
		properties: {
			severity: 'Minor' | 'Moderate' | 'Severe' | 'Extreme';
			event: string;
			description: string;
			instruction: string;
		};
	}[];
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

export interface NwsAlertResponse {
	features: {
		id?: string;
		properties: {
			severity: 'Minor' | 'Moderate' | 'Severe' | 'Extreme';
			event: string;
			description: string;
			instruction: string;
			sent?: string;
			headline?: string;
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
			fromdate?: string;
			url?: {
				report?: string;
			};
		};
		geometry: {
			coordinates: [number, number]; // [lon, lat]
		};
	}[];
}

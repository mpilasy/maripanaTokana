import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('./shared', () => ({
	getLocationInfo: vi.fn(async () => ({ countryCode: 'us', stateCode: null, subdivisionName: null })),
	calculateDistance: vi.fn(),
	USER_AGENT: 'test'
}));
vi.mock('./nws', () => ({ fetchNwsAlerts: vi.fn(), isInUS: () => true }));
vi.mock('./gdacs', () => ({ fetchGdacsAlerts: vi.fn() }));
vi.mock('./meteoAlarm', () => ({ fetchMeteoAlarmAlerts: vi.fn(), METEOALARM_COUNTRIES: new Set() }));
vi.mock('./jma', () => ({ fetchJmaAlerts: vi.fn(), isInJapan: () => false }));
vi.mock('./eccc', () => ({ fetchEcccAlerts: vi.fn(), isInCanada: () => false }));
vi.mock('./bom', () => ({ fetchBomAlerts: vi.fn(), isInAustralia: () => false }));
vi.mock('./nhc', () => ({ fetchNhcAlerts: vi.fn() }));

import { fetchAllAlerts, type AlertSettings } from './index';
import { fetchNwsAlerts } from './nws';
import { fetchMeteoAlarmAlerts } from './meteoAlarm';
import { fetchJmaAlerts } from './jma';
import { fetchNhcAlerts } from './nhc';

const base: AlertSettings = {
	alertsEnabled: true,
	alertsNwsEnabled: true,
	alertsGdacsEnabled: false,
	alertsMeteoAlarmEnabled: false,
	alertsJmaEnabled: false,
	alertsEcccEnabled: false,
	alertsBomEnabled: false,
	alertsNhcEnabled: false
};

const nwsAlert = { level: 'warning', title: 'Flood', description: '', source: 'nws' } as const;

describe('fetchAllAlerts', () => {
	beforeEach(() => {
		vi.clearAllMocks();
		vi.spyOn(console, 'warn').mockImplementation(() => {});
	});

	it('keeps alerts from successful sources and reports a rejecting enabled source', async () => {
		vi.mocked(fetchNwsAlerts).mockResolvedValue([nwsAlert, nwsAlert]);
		vi.mocked(fetchNhcAlerts).mockRejectedValue(new Error('HTTP 500'));
		const r = await fetchAllAlerts(40, -100, { ...base, alertsNhcEnabled: true });
		expect(r.alerts).toEqual([nwsAlert]);
		expect(r.failedSources).toEqual(['NHC']);
	});

	it('does not report disabled sources as failed', async () => {
		vi.mocked(fetchNwsAlerts).mockResolvedValue([]);
		vi.mocked(fetchJmaAlerts).mockRejectedValue(new Error('x'));
		vi.mocked(fetchMeteoAlarmAlerts).mockRejectedValue(new Error('x'));
		const r = await fetchAllAlerts(40, -100, base);
		expect(r.failedSources).toEqual([]);
		expect(fetchJmaAlerts).not.toHaveBeenCalled();
	});

	it('treats a valid empty result as success', async () => {
		vi.mocked(fetchNwsAlerts).mockResolvedValue([]);
		expect(await fetchAllAlerts(40, -100, base)).toEqual({ alerts: [], failedSources: [] });
	});
});

import {useState, useCallback, useRef, useEffect} from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';
import {WeatherData} from '../domain/WeatherData';
import {fetchWeather} from '../data/OpenMeteoApi';
import {
  getLastLocation,
  getFreshLocation,
  reverseGeocode,
  Coords,
} from '../services/LocationService';
import {supportedLocales} from '../i18n';
import {fontPairings} from '../theme/fonts';

export type UiState =
  | {type: 'permission_required'}
  | {type: 'loading'}
  | {type: 'success'; data: WeatherData}
  | {type: 'error'; messageKey: string};

const PREFS_KEY = 'widget_prefs';
const STALE_THRESHOLD_MS = 30 * 60 * 1000; // 30 minutes

interface StoredPrefs {
  metric_primary: boolean;
  font_index: number;
  locale_index: number;
  lat: number | null;
  lon: number | null;
  last_render_lat: number | null;
  last_render_lon: number | null;
  location_name: string | null;
}

const defaultPrefs: StoredPrefs = {
  metric_primary: true,
  font_index: 0,
  locale_index: 0,
  lat: null,
  lon: null,
  last_render_lat: null,
  last_render_lon: null,
  location_name: null,
};

async function loadPrefs(): Promise<StoredPrefs> {
  try {
    const raw = await AsyncStorage.getItem(PREFS_KEY);
    if (raw) return {...defaultPrefs, ...JSON.parse(raw)};
  } catch {}
  return defaultPrefs;
}

async function savePrefs(prefs: Partial<StoredPrefs>): Promise<void> {
  try {
    const current = await loadPrefs();
    await AsyncStorage.setItem(PREFS_KEY, JSON.stringify({...current, ...prefs}));
  } catch {}
}

function movedSignificantly(
  lat: number,
  lon: number,
  oldLat: number | null,
  oldLon: number | null,
): boolean {
  if (oldLat == null || oldLon == null) return true;
  const dlat = lat - oldLat;
  const dlon = lon - oldLon;
  // ~5 km threshold (0.045 degrees latitude ≈ 5 km)
  return dlat * dlat + dlon * dlon > 0.045 * 0.045;
}

export function useWeatherStore() {
  const [uiState, setUiState] = useState<UiState>({type: 'permission_required'});
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [metricPrimary, setMetricPrimary] = useState(true);
  const [fontIndex, setFontIndex] = useState(0);
  const [localeIndex, setLocaleIndex] = useState(0);
  const [prefsLoaded, setPrefsLoaded] = useState(false);
  const prefsRef = useRef<StoredPrefs>(defaultPrefs);

  // Load preferences on mount
  useEffect(() => {
    loadPrefs().then(prefs => {
      prefsRef.current = prefs;
      setMetricPrimary(prefs.metric_primary);
      setFontIndex(Math.min(prefs.font_index, fontPairings.length - 1));
      setLocaleIndex(Math.min(prefs.locale_index, supportedLocales.length - 1));
      setPrefsLoaded(true);
    });
  }, []);

  const doFetch = useCallback(async () => {
    const prefs = prefsRef.current;
    let usedCached = false;

    // Step 1: try cached location for instant render
    try {
      const cached: Coords = await getLastLocation();
      usedCached = true;
      const locationName = await reverseGeocode(cached.latitude, cached.longitude);
      const data = await fetchWeather(cached.latitude, cached.longitude, locationName);
      setUiState({type: 'success', data});
      await savePrefs({
        lat: cached.latitude,
        lon: cached.longitude,
        last_render_lat: cached.latitude,
        last_render_lon: cached.longitude,
        location_name: locationName,
      });
      prefsRef.current = {
        ...prefsRef.current,
        lat: cached.latitude,
        lon: cached.longitude,
        last_render_lat: cached.latitude,
        last_render_lon: cached.longitude,
        location_name: locationName,
      };
    } catch {
      // Cached location failed, continue to fresh
    }

    // Step 2: get fresh location, re-fetch if moved significantly
    try {
      const fresh: Coords = await getFreshLocation();
      const shouldRefetch =
        !usedCached ||
        movedSignificantly(
          fresh.latitude,
          fresh.longitude,
          prefsRef.current.last_render_lat,
          prefsRef.current.last_render_lon,
        );

      if (shouldRefetch) {
        const locationName = await reverseGeocode(fresh.latitude, fresh.longitude);
        const data = await fetchWeather(fresh.latitude, fresh.longitude, locationName);
        setUiState({type: 'success', data});
        await savePrefs({
          lat: fresh.latitude,
          lon: fresh.longitude,
          last_render_lat: fresh.latitude,
          last_render_lon: fresh.longitude,
          location_name: locationName,
        });
        prefsRef.current = {
          ...prefsRef.current,
          lat: fresh.latitude,
          lon: fresh.longitude,
          last_render_lat: fresh.latitude,
          last_render_lon: fresh.longitude,
          location_name: locationName,
        };
      }
    } catch {
      if (!usedCached) {
        setUiState({type: 'error', messageKey: 'error_get_location'});
      }
    }
  }, []);

  const fetchWeatherAction = useCallback(async () => {
    setUiState({type: 'loading'});
    await doFetch();
  }, [doFetch]);

  const refresh = useCallback(async () => {
    setIsRefreshing(true);
    await doFetch();
    setIsRefreshing(false);
  }, [doFetch]);

  const refreshIfStale = useCallback(() => {
    if (uiState.type === 'success') {
      const age = Date.now() - uiState.data.timestamp;
      if (age > STALE_THRESHOLD_MS) {
        refresh();
      }
    }
  }, [uiState, refresh]);

  const toggleUnits = useCallback(() => {
    setMetricPrimary(prev => {
      const newValue = !prev;
      savePrefs({metric_primary: newValue});
      return newValue;
    });
  }, []);

  const cycleFont = useCallback(() => {
    setFontIndex(prev => {
      const newIndex = (prev + 1) % fontPairings.length;
      savePrefs({font_index: newIndex});
      return newIndex;
    });
  }, []);

  const cycleLanguage = useCallback(() => {
    setLocaleIndex(prev => {
      const newIndex = (prev + 1) % supportedLocales.length;
      savePrefs({locale_index: newIndex});
      return newIndex;
    });
  }, []);

  return {
    uiState,
    isRefreshing,
    metricPrimary,
    fontIndex,
    localeIndex,
    prefsLoaded,
    fetchWeather: fetchWeatherAction,
    refresh,
    refreshIfStale,
    toggleUnits,
    cycleFont,
    cycleLanguage,
    setUiState,
  };
}

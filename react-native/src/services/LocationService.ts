import Geolocation from '@react-native-community/geolocation';
import {Platform, PermissionsAndroid} from 'react-native';

export interface Coords {
  latitude: number;
  longitude: number;
}

/** Request location permissions on Android. Returns true if granted. */
export async function requestLocationPermission(): Promise<boolean> {
  if (Platform.OS === 'ios') {
    return new Promise(resolve => {
      Geolocation.requestAuthorization(
        () => resolve(true),
        () => resolve(false),
      );
    });
  }

  try {
    const fineGranted = await PermissionsAndroid.request(
      PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
    );
    return fineGranted === PermissionsAndroid.RESULTS.GRANTED;
  } catch {
    return false;
  }
}

/** Check if location permission is already granted. */
export async function hasLocationPermission(): Promise<boolean> {
  if (Platform.OS === 'ios') {
    return true; // iOS handles via requestAuthorization
  }
  return PermissionsAndroid.check(
    PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
  );
}

/** Get the last known (cached) location. Fast but may be stale. */
export function getLastLocation(): Promise<Coords> {
  return new Promise((resolve, reject) => {
    Geolocation.getCurrentPosition(
      position => {
        resolve({
          latitude: position.coords.latitude,
          longitude: position.coords.longitude,
        });
      },
      error => reject(error),
      {enableHighAccuracy: false, timeout: 5000, maximumAge: 600000},
    );
  });
}

/** Get a fresh GPS location. Slower but accurate. */
export function getFreshLocation(): Promise<Coords> {
  return new Promise((resolve, reject) => {
    Geolocation.getCurrentPosition(
      position => {
        resolve({
          latitude: position.coords.latitude,
          longitude: position.coords.longitude,
        });
      },
      error => reject(error),
      {enableHighAccuracy: true, timeout: 15000, maximumAge: 0},
    );
  });
}

/**
 * Reverse-geocode coordinates to a city name.
 * Uses Open-Meteo geocoding API (free, no key required).
 */
export async function reverseGeocode(lat: number, lon: number): Promise<string> {
  try {
    const url = `https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lon}&zoom=10&addressdetails=1`;
    const response = await fetch(url, {
      headers: {'User-Agent': 'maripanaTokana/1.0'},
    });
    if (!response.ok) return `${lat.toFixed(2)}, ${lon.toFixed(2)}`;
    const data = await response.json();
    const address = data.address;
    return address?.city
      || address?.town
      || address?.village
      || address?.municipality
      || address?.county
      || `${lat.toFixed(2)}, ${lon.toFixed(2)}`;
  } catch {
    return `${lat.toFixed(2)}, ${lon.toFixed(2)}`;
  }
}

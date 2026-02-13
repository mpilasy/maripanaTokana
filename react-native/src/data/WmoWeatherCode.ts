/** Maps WMO weather interpretation codes to i18n string keys. */
export function wmoDescriptionKey(code: number): string {
  switch (code) {
    case 0: return 'wmo_clear_sky';
    case 1: return 'wmo_mainly_clear';
    case 2: return 'wmo_partly_cloudy';
    case 3: return 'wmo_overcast';
    case 45: return 'wmo_fog';
    case 48: return 'wmo_rime_fog';
    case 51: return 'wmo_light_drizzle';
    case 53: return 'wmo_moderate_drizzle';
    case 55: return 'wmo_dense_drizzle';
    case 56: return 'wmo_light_freezing_drizzle';
    case 57: return 'wmo_dense_freezing_drizzle';
    case 61: return 'wmo_slight_rain';
    case 63: return 'wmo_moderate_rain';
    case 65: return 'wmo_heavy_rain';
    case 66: return 'wmo_light_freezing_rain';
    case 67: return 'wmo_heavy_freezing_rain';
    case 71: return 'wmo_slight_snowfall';
    case 73: return 'wmo_moderate_snowfall';
    case 75: return 'wmo_heavy_snowfall';
    case 77: return 'wmo_snow_grains';
    case 80: return 'wmo_slight_rain_showers';
    case 81: return 'wmo_moderate_rain_showers';
    case 82: return 'wmo_violent_rain_showers';
    case 85: return 'wmo_slight_snow_showers';
    case 86: return 'wmo_heavy_snow_showers';
    case 95: return 'wmo_thunderstorm';
    case 96: return 'wmo_thunderstorm_slight_hail';
    case 99: return 'wmo_thunderstorm_heavy_hail';
    default: return 'wmo_unknown';
  }
}

/** Returns emoji based on WMO weather code + time of day. */
export function wmoEmoji(code: number, isNight: boolean = false): string {
  switch (code) {
    case 0:
      return isNight ? '\uD83C\uDF11' : '\u2600\uFE0F';     // 🌑 or ☀️
    case 1:
      return isNight ? '\uD83C\uDF14' : '\uD83C\uDF24\uFE0F'; // 🌔 or 🌤️
    case 2:
      return isNight ? '\uD83C\uDF13' : '\u26C5';            // 🌓 or ⛅
    case 3:
      return '\u2601\uFE0F';          // ☁️
    case 45:
    case 48:
      return '\uD83C\uDF2B\uFE0F';    // 🌫️
    case 51:
    case 53:
    case 55:
      return '\uD83C\uDF26\uFE0F';    // 🌦️
    case 56:
    case 57:
      return '\uD83C\uDF28\uFE0F';    // 🌨️
    case 61:
    case 63:
    case 65:
      return '\uD83C\uDF27\uFE0F';    // 🌧️
    case 66:
    case 67:
      return '\uD83C\uDF28\uFE0F';    // 🌨️
    case 71:
    case 73:
    case 75:
    case 77:
      return '\u2744\uFE0F';          // ❄️
    case 80:
    case 81:
    case 82:
      return '\uD83C\uDF26\uFE0F';    // 🌦️
    case 85:
    case 86:
      return '\uD83C\uDF28\uFE0F';    // 🌨️
    case 95:
    case 96:
    case 99:
      return '\u26C8\uFE0F';          // ⛈️
    default:
      return '\uD83C\uDF10';          // 🌐
  }
}

/** Maps WMO weather code + is_day to an OWM-style icon code. */
export function wmoIconCode(code: number, isDay: boolean): string {
  let base: string;
  switch (code) {
    case 0: base = '01'; break;
    case 1: base = '02'; break;
    case 2: base = '03'; break;
    case 3: base = '04'; break;
    case 45: case 48: base = '50'; break;
    case 51: case 53: case 55: case 56: case 57: base = '09'; break;
    case 61: case 63: case 65: case 66: case 67: base = '10'; break;
    case 71: case 73: case 75: case 77: case 85: case 86: base = '13'; break;
    case 80: case 81: case 82: base = '09'; break;
    case 95: case 96: case 99: base = '11'; break;
    default: base = '01'; break;
  }
  return `${base}${isDay ? 'd' : 'n'}`;
}

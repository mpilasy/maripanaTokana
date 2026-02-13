import {Dimensions} from 'react-native';

/** Get responsive scale factor based on screen width, matching Android app behavior. */
export function getScale(): number {
  const {width} = Dimensions.get('window');
  // dp-like conversion (React Native uses dp on Android)
  if (width >= 400) return 1.0;
  if (width >= 320) return 0.8;
  return 0.7;
}

/** Scale a font size by the current scale factor. */
export function sf(baseFontSize: number, scale: number): number {
  return baseFontSize * scale;
}

/** Scale a spacing/dimension value by the current scale factor. */
export function sd(baseDp: number, scale: number): number {
  return baseDp * scale;
}

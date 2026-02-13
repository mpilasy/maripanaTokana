import React from 'react';
import {View, Text, StyleSheet} from 'react-native';
import {DailyForecast} from '../domain/WeatherData';
import {wmoEmoji, wmoDescriptionKey} from '../data/WmoWeatherCode';
import {DualUnitText} from './DualUnitText';
import {formatDayName, formatDayMonth} from '../utils/dateFormat';
import {t} from '../i18n';
import {Colors} from '../theme/colors';

interface DailyForecastListProps {
  forecasts: DailyForecast[];
  metricPrimary: boolean;
  localizeDigits: (s: string) => string;
  onToggleUnits: () => void;
  localeTag: string;
  scale: number;
  bodyFont?: string;
  displayFont?: string;
}

export function DailyForecastList({
  forecasts,
  metricPrimary,
  localizeDigits,
  onToggleUnits,
  localeTag,
  scale,
  bodyFont,
  displayFont,
}: DailyForecastListProps) {
  return (
    <View style={{gap: 8}}>
      {forecasts.map((item, index) => {
        const [maxP, maxS] = item.tempMax.displayDual(metricPrimary);
        const [minP, minS] = item.tempMin.displayDual(metricPrimary);
        const desc = t(wmoDescriptionKey(item.weatherCode), localeTag);

        return (
          <View key={index} style={[styles.row, {paddingHorizontal: 16 * scale, paddingVertical: 12 * scale}]}>
            <View style={{width: 100 * scale}}>
              <Text
                style={[
                  styles.dayName,
                  {fontSize: 14 * scale, fontFamily: bodyFont},
                ]}>
                {formatDayName(item.date, localeTag)}
              </Text>
              <Text
                style={[
                  styles.dayMonth,
                  {fontSize: 10 * scale, fontFamily: bodyFont},
                ]}>
                {localizeDigits(formatDayMonth(item.date, localeTag))}
              </Text>
            </View>
            <Text
              style={[
                styles.description,
                {fontSize: 12 * scale, fontFamily: bodyFont},
              ]}
              numberOfLines={1}>
              {wmoEmoji(item.weatherCode)} {desc}
            </Text>
            <Text
              style={[
                styles.precip,
                {fontSize: 11 * scale, fontFamily: bodyFont},
              ]}>
              {item.precipProbability > 0
                ? localizeDigits(`${item.precipProbability}%`)
                : ''}
            </Text>
            <View style={{width: 8}} />
            <DualUnitText
              primary={localizeDigits(`\u2191${maxP} \u2193${minP}`)}
              secondary={localizeDigits(`\u2191${maxS} \u2193${minS}`)}
              primarySize={13 * scale}
              displayFont={displayFont}
              onPress={onToggleUnits}
            />
          </View>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Colors.cardSurfaceSubtle,
    borderRadius: 12,
  },
  dayName: {
    fontWeight: '500',
    color: Colors.onSurface,
  },
  dayMonth: {
    color: Colors.onSurfaceQuaternary,
  },
  description: {
    flex: 1,
    color: Colors.onSurfaceSecondary,
  },
  precip: {
    color: Colors.precipBlue,
  },
});

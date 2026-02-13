import React from 'react';
import {View, Text, ScrollView, StyleSheet} from 'react-native';
import {HourlyForecast} from '../domain/WeatherData';
import {wmoEmoji} from '../data/WmoWeatherCode';
import {DualUnitText} from './DualUnitText';
import {formatHourMinute} from '../utils/dateFormat';
import {Colors} from '../theme/colors';

interface HourlyForecastRowProps {
  forecasts: HourlyForecast[];
  metricPrimary: boolean;
  dailySunrise: number[];
  dailySunset: number[];
  localizeDigits: (s: string) => string;
  onToggleUnits: () => void;
  scale: number;
  bodyFont?: string;
  displayFont?: string;
}

export function HourlyForecastRow({
  forecasts,
  metricPrimary,
  dailySunrise,
  dailySunset,
  localizeDigits,
  onToggleUnits,
  scale,
  bodyFont,
  displayFont,
}: HourlyForecastRowProps) {
  return (
    <ScrollView
      horizontal
      showsHorizontalScrollIndicator={false}
      contentContainerStyle={{paddingVertical: 8, gap: 12 * scale}}>
      {forecasts.map((item, index) => {
        const dayIdx = Math.max(
          0,
          dailySunrise.reduce(
            (acc, sr, i) => (sr <= item.time ? i : acc),
            0,
          ),
        );
        const sr = dailySunrise[dayIdx] ?? 0;
        const ss = dailySunset[dayIdx] ?? 0;
        const isNight = item.time < sr || item.time > ss;
        const [tempP, tempS] = item.temperature.displayDual(metricPrimary);

        return (
          <View key={index} style={[styles.card, {padding: 12 * scale}]}>
            <Text
              style={[
                styles.time,
                {fontSize: 12 * scale, fontFamily: bodyFont},
              ]}>
              {localizeDigits(formatHourMinute(item.time))}
            </Text>
            <Text style={{fontSize: 20 * scale, marginVertical: 4}}>
              {wmoEmoji(item.weatherCode, isNight)}
            </Text>
            <DualUnitText
              primary={localizeDigits(tempP)}
              secondary={localizeDigits(tempS)}
              primarySize={14 * scale}
              displayFont={displayFont}
              onPress={onToggleUnits}
            />
            <Text
              style={[
                styles.precip,
                {fontSize: 11 * scale, fontFamily: bodyFont},
              ]}>
              {item.precipProbability > 0
                ? localizeDigits(`${item.precipProbability}%`)
                : ''}
            </Text>
          </View>
        );
      })}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: Colors.cardSurfaceTranslucent,
    borderRadius: 16,
    alignItems: 'center',
  },
  time: {
    color: Colors.onSurfaceSecondary,
  },
  precip: {
    color: Colors.precipBlue,
    marginTop: 4,
  },
});

import React from 'react';
import {View, Text, StyleSheet} from 'react-native';
import {DualUnitText} from './DualUnitText';
import {Colors} from '../theme/colors';

interface DetailCardProps {
  title: string;
  value: string;
  secondaryValue?: string;
  subtitle?: string;
  onToggleUnits?: () => void;
  scale: number;
  bodyFont?: string;
  displayFont?: string;
  style?: object;
}

export function DetailCard({
  title,
  value,
  secondaryValue,
  subtitle,
  onToggleUnits,
  scale,
  bodyFont,
  displayFont,
  style,
}: DetailCardProps) {
  return (
    <View style={[styles.card, {padding: 16 * scale}, style]}>
      <Text
        style={[styles.title, {fontSize: 14 * scale, fontFamily: bodyFont}]}>
        {title}
      </Text>
      <View style={{height: 8}} />
      {secondaryValue ? (
        <DualUnitText
          primary={value}
          secondary={secondaryValue}
          primarySize={20 * scale}
          displayFont={displayFont}
          onPress={onToggleUnits}
        />
      ) : (
        <Text
          style={[
            styles.value,
            {fontSize: 20 * scale, fontFamily: displayFont},
          ]}>
          {value}
        </Text>
      )}
      {subtitle && (
        <Text
          style={[
            styles.subtitle,
            {fontSize: 12 * scale, fontFamily: bodyFont},
          ]}>
          {subtitle}
        </Text>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: Colors.cardSurfaceTranslucent,
    borderRadius: 16,
  },
  title: {
    color: Colors.onSurfaceSecondary,
  },
  value: {
    fontWeight: 'bold',
    color: Colors.onSurface,
  },
  subtitle: {
    color: Colors.onSurfaceTertiary,
    marginTop: 4,
  },
});

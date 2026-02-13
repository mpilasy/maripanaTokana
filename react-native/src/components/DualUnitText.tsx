import React from 'react';
import {View, Text, TouchableOpacity, StyleSheet} from 'react-native';
import {Colors} from '../theme/colors';

interface DualUnitTextProps {
  primary: string;
  secondary: string;
  primarySize?: number;
  color?: string;
  align?: 'flex-start' | 'flex-end' | 'center';
  displayFont?: string;
  onPress?: () => void;
}

export function DualUnitText({
  primary,
  secondary,
  primarySize = 16,
  color = Colors.onSurface,
  align = 'flex-start',
  displayFont,
  onPress,
}: DualUnitTextProps) {
  const content = (
    <View style={{alignItems: align}}>
      <Text
        style={[
          styles.primary,
          {
            fontSize: primarySize,
            color,
            fontFamily: displayFont,
          },
        ]}>
        {primary}
      </Text>
      <Text
        style={[
          styles.secondary,
          {
            fontSize: primarySize * 0.75,
            color: color + '8C', // ~55% alpha
            fontFamily: displayFont,
          },
        ]}>
        {secondary}
      </Text>
    </View>
  );

  if (onPress) {
    return <TouchableOpacity onPress={onPress}>{content}</TouchableOpacity>;
  }
  return content;
}

const styles = StyleSheet.create({
  primary: {
    fontWeight: 'bold',
  },
  secondary: {
    fontWeight: 'normal',
  },
});

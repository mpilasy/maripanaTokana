import React, {useState, useRef} from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  Animated,
  StyleSheet,
  LayoutAnimation,
  Platform,
  UIManager,
} from 'react-native';
import {Colors} from '../theme/colors';

// Enable LayoutAnimation on Android
if (Platform.OS === 'android' && UIManager.setLayoutAnimationEnabledExperimental) {
  UIManager.setLayoutAnimationEnabledExperimental(true);
}

interface CollapsibleSectionProps {
  title: string;
  initialExpanded?: boolean;
  scale: number;
  bodyFont?: string;
  children: React.ReactNode;
}

export function CollapsibleSection({
  title,
  initialExpanded = false,
  scale,
  bodyFont,
  children,
}: CollapsibleSectionProps) {
  const [expanded, setExpanded] = useState(initialExpanded);
  const rotateAnim = useRef(new Animated.Value(initialExpanded ? 1 : 0)).current;

  const toggle = () => {
    LayoutAnimation.configureNext(LayoutAnimation.Presets.easeInEaseOut);
    const next = !expanded;
    setExpanded(next);
    Animated.timing(rotateAnim, {
      toValue: next ? 1 : 0,
      duration: 200,
      useNativeDriver: true,
    }).start();
  };

  const rotation = rotateAnim.interpolate({
    inputRange: [0, 1],
    outputRange: ['0deg', '180deg'],
  });

  return (
    <View>
      <TouchableOpacity onPress={toggle} style={styles.header}>
        <Text
          style={[
            styles.title,
            {fontSize: 20 * scale, fontFamily: bodyFont},
          ]}>
          {title}
        </Text>
        <View style={{flex: 1}} />
        <Animated.Text
          style={[
            styles.chevron,
            {transform: [{rotate: rotation}]},
          ]}>
          {'\u25BC'}
        </Animated.Text>
      </TouchableOpacity>
      {expanded && <View>{children}</View>}
    </View>
  );
}

const styles = StyleSheet.create({
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 8,
  },
  title: {
    fontWeight: 'bold',
    color: Colors.onSurface,
  },
  chevron: {
    color: Colors.onSurfaceSecondary,
    fontSize: 14,
  },
});

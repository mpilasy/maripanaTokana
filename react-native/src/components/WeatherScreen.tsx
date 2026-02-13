import React, {useEffect, useCallback} from 'react';
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  ActivityIndicator,
  RefreshControl,
  StyleSheet,
  StatusBar,
  AppState,
  Linking,
  I18nManager,
} from 'react-native';
import {useWeatherStore} from '../hooks/useWeatherStore';
import {WeatherData} from '../domain/WeatherData';
import {wmoEmoji, wmoDescriptionKey} from '../data/WmoWeatherCode';
import {DualUnitText} from './DualUnitText';
import {CollapsibleSection} from './CollapsibleSection';
import {HourlyForecastRow} from './HourlyForecastRow';
import {DailyForecastList} from './DailyForecastList';
import {DetailsContent} from './DetailsContent';
import {t, tArray, localizeDigits as ld, supportedLocales} from '../i18n';
import {fontPairings} from '../theme/fonts';
import {Colors} from '../theme/colors';
import {getScale} from '../theme/scaling';
import {formatFullDate, formatTime} from '../utils/dateFormat';
import {requestLocationPermission, hasLocationPermission} from '../services/LocationService';

export function WeatherScreen() {
  const store = useWeatherStore();
  const {
    uiState,
    isRefreshing,
    metricPrimary,
    fontIndex,
    localeIndex,
    prefsLoaded,
    fetchWeather,
    refresh,
    refreshIfStale,
    toggleUnits,
    cycleFont,
    cycleLanguage,
    setUiState,
  } = store;

  const locale = supportedLocales[localeIndex];
  const localeTag = locale.tag;
  const pairing = fontPairings[fontIndex];
  const scale = getScale();
  const displayFont = pairing.display === 'System' ? undefined : pairing.display;
  const bodyFont = pairing.body === 'System' ? undefined : pairing.body;

  const localizeDigitsForLocale = useCallback(
    (s: string) => ld(s, locale),
    [locale],
  );

  // Handle permissions and initial fetch
  useEffect(() => {
    if (!prefsLoaded) return;
    (async () => {
      const granted = await hasLocationPermission();
      if (granted) {
        fetchWeather();
      } else {
        setUiState({type: 'permission_required'});
      }
    })();
  }, [prefsLoaded, fetchWeather, setUiState]);

  // Auto-refresh on app resume
  useEffect(() => {
    const sub = AppState.addEventListener('change', nextState => {
      if (nextState === 'active') {
        refreshIfStale();
      }
    });
    return () => sub.remove();
  }, [refreshIfStale]);

  // Handle RTL for Arabic
  useEffect(() => {
    I18nManager.allowRTL(true);
    I18nManager.forceRTL(locale.rtl);
  }, [locale.rtl]);

  const handleGrantPermission = async () => {
    const granted = await requestLocationPermission();
    if (granted) {
      fetchWeather();
    }
  };

  return (
    <View style={styles.container}>
      <StatusBar barStyle="light-content" backgroundColor={Colors.backgroundDark} />

      {uiState.type === 'loading' && (
        <View style={styles.centered}>
          <ActivityIndicator size="large" color={Colors.onSurface} />
        </View>
      )}

      {uiState.type === 'permission_required' && (
        <View style={styles.centered}>
          <Text style={[styles.permTitle, {fontSize: 22 * scale, fontFamily: displayFont}]}>
            {t('permission_title', localeTag)}
          </Text>
          <View style={{height: 16}} />
          <Text style={[styles.permMessage, {fontSize: 16 * scale, fontFamily: bodyFont}]}>
            {t('permission_message', localeTag)}
          </Text>
          <View style={{height: 24}} />
          <TouchableOpacity style={styles.button} onPress={handleGrantPermission}>
            <Text style={[styles.buttonText, {fontFamily: bodyFont}]}>
              {t('grant_permission', localeTag)}
            </Text>
          </TouchableOpacity>
        </View>
      )}

      {uiState.type === 'error' && (
        <View style={styles.centered}>
          <Text style={[styles.permTitle, {fontSize: 22 * scale, fontFamily: displayFont}]}>
            {t('error_title', localeTag)}
          </Text>
          <View style={{height: 16}} />
          <Text style={[styles.permMessage, {fontSize: 16 * scale, fontFamily: bodyFont}]}>
            {t(uiState.messageKey, localeTag)}
          </Text>
          <View style={{height: 24}} />
          <TouchableOpacity style={styles.button} onPress={fetchWeather}>
            <Text style={[styles.buttonText, {fontFamily: bodyFont}]}>
              {t('error_retry', localeTag)}
            </Text>
          </TouchableOpacity>
        </View>
      )}

      {uiState.type === 'success' && (
        <WeatherContent
          data={uiState.data}
          metricPrimary={metricPrimary}
          fontName={pairing.name}
          currentFlag={locale.flag}
          localizeDigits={localizeDigitsForLocale}
          localeTag={localeTag}
          onToggleUnits={toggleUnits}
          onCycleFont={cycleFont}
          onCycleLanguage={cycleLanguage}
          isRefreshing={isRefreshing}
          onRefresh={refresh}
          scale={scale}
          displayFont={displayFont}
          bodyFont={bodyFont}
        />
      )}
    </View>
  );
}

// --- WeatherContent ---

interface WeatherContentProps {
  data: WeatherData;
  metricPrimary: boolean;
  fontName: string;
  currentFlag: string;
  localizeDigits: (s: string) => string;
  localeTag: string;
  onToggleUnits: () => void;
  onCycleFont: () => void;
  onCycleLanguage: () => void;
  isRefreshing: boolean;
  onRefresh: () => void;
  scale: number;
  displayFont?: string;
  bodyFont?: string;
}

function WeatherContent({
  data,
  metricPrimary,
  fontName,
  currentFlag,
  localizeDigits,
  localeTag,
  onToggleUnits,
  onCycleFont,
  onCycleLanguage,
  isRefreshing,
  onRefresh,
  scale,
  displayFont,
  bodyFont,
}: WeatherContentProps) {
  const isNight = data.timestamp < data.sunrise * 1000 || data.timestamp > data.sunset * 1000;
  const [tempPrimary, tempSecondary] = data.temperature.displayDualMixed(metricPrimary);
  const [flPrimary, flSecondary] = data.feelsLike.displayDual(metricPrimary);
  const [maxP, maxS] = data.tempMax.displayDual(metricPrimary);
  const [minP, minS] = data.tempMin.displayDual(metricPrimary);
  const [windP, windS] = data.windSpeed.displayDual(metricPrimary);
  const directions = tArray('cardinal_directions', localeTag);
  const dirIndex = ((data.windDeg % 360 + 360) % 360 * 16 / 360) % 16 | 0;
  const weatherDescription = t(wmoDescriptionKey(data.weatherCode), localeTag);

  return (
    <View style={[styles.contentContainer, {paddingHorizontal: 24 * scale}]}>
      {/* Fixed Header */}
      <View style={{paddingTop: 24 * scale + (StatusBar.currentHeight ?? 44)}}>
        <Text style={[styles.locationName, {fontSize: 32 * scale, fontFamily: displayFont}]}>
          {data.locationName}
        </Text>
        <Text style={[styles.date, {fontSize: 16 * scale, fontFamily: bodyFont}]}>
          {localizeDigits(formatFullDate(data.timestamp, localeTag))}
        </Text>
        <Text style={[styles.updated, {fontSize: 12 * scale, fontFamily: bodyFont}]}>
          {localizeDigits(t('updated_time', localeTag, {time: formatTime(data.timestamp)}))}
        </Text>
      </View>

      {/* Scrollable Content */}
      <ScrollView
        style={{flex: 1}}
        contentContainerStyle={{paddingVertical: 24 * scale}}
        showsVerticalScrollIndicator={false}
        refreshControl={
          <RefreshControl
            refreshing={isRefreshing}
            onRefresh={onRefresh}
            tintColor={Colors.onSurface}
            colors={[Colors.onSurface]}
          />
        }>
        {/* Hero Card */}
        <View style={styles.heroCard}>
          <View style={{padding: 24 * scale}}>
            {/* Top row: emoji+description (left) + temperature (right) */}
            <View style={styles.heroTopRow}>
              <View style={{flex: 1, alignItems: 'center'}}>
                <Text style={{fontSize: 48 * scale}}>
                  {wmoEmoji(data.weatherCode, isNight)}
                </Text>
                <View style={{height: 4}} />
                <Text
                  style={[
                    styles.weatherDesc,
                    {fontSize: 16 * scale, fontFamily: bodyFont},
                  ]}>
                  {weatherDescription}
                </Text>
              </View>
              <DualUnitText
                primary={localizeDigits(tempPrimary)}
                secondary={localizeDigits(tempSecondary)}
                primarySize={48 * scale}
                align="flex-end"
                displayFont={displayFont}
                onPress={onToggleUnits}
              />
            </View>

            <View style={{height: 16}} />

            {/* Second row: feels like (left) + precipitation (right) */}
            <View style={styles.heroRow}>
              <View style={{flex: 1}}>
                <Text style={[styles.feelsLikeLabel, {fontSize: 14 * scale, fontFamily: bodyFont}]}>
                  {t('feels_like', localeTag)}
                </Text>
                <DualUnitText
                  primary={localizeDigits(flPrimary)}
                  secondary={localizeDigits(flSecondary)}
                  displayFont={displayFont}
                  onPress={onToggleUnits}
                />
              </View>
              <View style={{alignItems: 'flex-end'}}>
                {data.snow ? (
                  (() => {
                    const [snowP, snowS] = data.snow.displayDual(metricPrimary);
                    return (
                      <DualUnitText
                        primary={`\u2744\uFE0F ${localizeDigits(snowP)}`}
                        secondary={localizeDigits(snowS)}
                        displayFont={displayFont}
                        onPress={onToggleUnits}
                      />
                    );
                  })()
                ) : data.rain ? (
                  (() => {
                    const [rainP, rainS] = data.rain.displayDual(metricPrimary);
                    return (
                      <DualUnitText
                        primary={`\uD83C\uDF27\uFE0F ${localizeDigits(rainP)}`}
                        secondary={localizeDigits(rainS)}
                        displayFont={displayFont}
                        onPress={onToggleUnits}
                      />
                    );
                  })()
                ) : (
                  <Text style={[styles.noPrecip, {fontSize: 14 * scale, fontFamily: bodyFont}]}>
                    {t('no_precip', localeTag)}
                  </Text>
                )}
              </View>
            </View>

            <View style={{height: 16}} />

            {/* Third row: high/low (left) + wind (right) */}
            <View style={styles.heroRow}>
              <View style={{flex: 1}}>
                <DualUnitText
                  primary={localizeDigits(`\u2191${maxP} / \u2193${minP}`)}
                  secondary={localizeDigits(`\u2191${maxS} / \u2193${minS}`)}
                  displayFont={displayFont}
                  onPress={onToggleUnits}
                />
              </View>
              <View style={{alignItems: 'flex-end'}}>
                <DualUnitText
                  primary={localizeDigits(`${windP} ${directions[dirIndex]}`)}
                  secondary={localizeDigits(windS)}
                  displayFont={displayFont}
                  onPress={onToggleUnits}
                />
              </View>
            </View>
          </View>
          <Text style={[styles.copyright, {fontSize: 9 * scale}]}>
            {'\u00A9'} Orinasa Njarasoa
          </Text>
        </View>

        <View style={{height: 24 * scale}} />

        {/* Hourly Forecast */}
        {data.hourlyForecast.length > 0 && (
          <>
            <CollapsibleSection
              title={t('section_hourly_forecast', localeTag)}
              initialExpanded={true}
              scale={scale}
              bodyFont={bodyFont}>
              <HourlyForecastRow
                forecasts={data.hourlyForecast}
                metricPrimary={metricPrimary}
                dailySunrise={data.dailySunrise}
                dailySunset={data.dailySunset}
                localizeDigits={localizeDigits}
                onToggleUnits={onToggleUnits}
                scale={scale}
                bodyFont={bodyFont}
                displayFont={displayFont}
              />
            </CollapsibleSection>
            <View style={{height: 24 * scale}} />
          </>
        )}

        {/* 10-Day Forecast */}
        {data.dailyForecast.length > 0 && (
          <>
            <CollapsibleSection
              title={t('section_this_week', localeTag)}
              scale={scale}
              bodyFont={bodyFont}>
              <DailyForecastList
                forecasts={data.dailyForecast}
                metricPrimary={metricPrimary}
                localizeDigits={localizeDigits}
                onToggleUnits={onToggleUnits}
                localeTag={localeTag}
                scale={scale}
                bodyFont={bodyFont}
                displayFont={displayFont}
              />
            </CollapsibleSection>
            <View style={{height: 24 * scale}} />
          </>
        )}

        {/* Current Conditions */}
        <CollapsibleSection
          title={t('section_current_conditions', localeTag)}
          scale={scale}
          bodyFont={bodyFont}>
          <DetailsContent
            data={data}
            metricPrimary={metricPrimary}
            localizeDigits={localizeDigits}
            onToggleUnits={onToggleUnits}
            localeTag={localeTag}
            scale={scale}
            bodyFont={bodyFont}
            displayFont={displayFont}
          />
        </CollapsibleSection>
      </ScrollView>

      {/* Fixed Footer */}
      <View style={styles.footer}>
        {/* Font selector */}
        <TouchableOpacity onPress={onCycleFont} style={styles.footerLeft}>
          <Text style={[styles.footerIcon, {fontSize: 14}]}>{'\uD83D\uDDA3'}</Text>
          <View style={{width: 4}} />
          <Text style={[styles.footerFontName, {fontSize: 9 * scale, fontFamily: bodyFont}]}>
            {fontName.replace(' + ', '\n')}
          </Text>
        </TouchableOpacity>

        <View style={{flex: 1}} />

        {/* Credits */}
        <View style={{alignItems: 'center'}}>
          <Text style={[styles.creditsText, {fontSize: 9 * scale}]}>
            {t('credits_weather_data', localeTag)}
            <Text
              style={styles.creditsLink}
              onPress={() => Linking.openURL('https://open-meteo.com')}>
              Open-Meteo
            </Text>
          </Text>
        </View>

        <View style={{flex: 1}} />

        {/* Language flag */}
        <TouchableOpacity onPress={onCycleLanguage}>
          <Text style={{fontSize: 16 * scale}}>{currentFlag}</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.backgroundDark,
  },
  centered: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 32,
  },
  contentContainer: {
    flex: 1,
  },
  permTitle: {
    fontWeight: 'bold',
    color: Colors.onSurface,
    textAlign: 'center',
  },
  permMessage: {
    color: Colors.onSurfaceSecondary,
    textAlign: 'center',
    paddingHorizontal: 16,
  },
  button: {
    backgroundColor: Colors.cardSurface,
    paddingHorizontal: 24,
    paddingVertical: 12,
    borderRadius: 8,
  },
  buttonText: {
    color: Colors.onSurface,
    fontSize: 16,
    fontWeight: '600',
  },
  locationName: {
    fontWeight: 'bold',
    color: Colors.onSurface,
  },
  date: {
    color: Colors.onSurfaceSecondary,
  },
  updated: {
    color: Colors.onSurfaceQuaternary,
  },
  heroCard: {
    backgroundColor: Colors.cardSurface,
    borderRadius: 20,
    overflow: 'hidden',
  },
  heroTopRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  heroRow: {
    flexDirection: 'row',
  },
  weatherDesc: {
    color: 'rgba(255,255,255,0.9)',
    textAlign: 'center',
  },
  feelsLikeLabel: {
    color: Colors.onSurfaceSecondary,
  },
  noPrecip: {
    color: Colors.onSurfaceDim,
  },
  copyright: {
    color: Colors.onSurfaceFaint,
    textAlign: 'center',
    paddingTop: 7,
    paddingBottom: 4,
  },
  footer: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingBottom: 8,
    paddingTop: 4,
  },
  footerLeft: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  footerIcon: {
    color: Colors.onSurfaceQuaternary,
  },
  footerFontName: {
    color: Colors.onSurfaceQuaternary,
    lineHeight: 11,
  },
  creditsText: {
    color: Colors.onSurfaceQuinary,
  },
  creditsLink: {
    color: Colors.onSurfaceDim,
    textDecorationLine: 'underline',
  },
});

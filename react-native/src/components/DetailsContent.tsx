import React from 'react';
import {View, Text, TouchableOpacity, StyleSheet} from 'react-native';
import {WeatherData} from '../domain/WeatherData';
import {DualUnitText} from './DualUnitText';
import {DetailCard} from './DetailCard';
import {t, tArray} from '../i18n';
import {formatTimeFromEpochSeconds} from '../utils/dateFormat';
import {Colors} from '../theme/colors';

interface DetailsContentProps {
  data: WeatherData;
  metricPrimary: boolean;
  localizeDigits: (s: string) => string;
  onToggleUnits: () => void;
  localeTag: string;
  scale: number;
  bodyFont?: string;
  displayFont?: string;
}

export function DetailsContent({
  data,
  metricPrimary,
  localizeDigits,
  onToggleUnits,
  localeTag,
  scale,
  bodyFont,
  displayFont,
}: DetailsContentProps) {
  const directions = tArray('cardinal_directions', localeTag);
  const uvLabels = tArray('uv_labels', localeTag);
  const [tempP, tempS] = data.temperature.displayDual(metricPrimary);
  const [flP] = data.feelsLike.displayDual(metricPrimary);
  const [pressP, pressS] = data.pressure.displayDual(metricPrimary);
  const [dewP, dewS] = data.dewPoint.displayDual(metricPrimary);
  const [windP, windS] = data.windSpeed.displayDual(metricPrimary);
  const [minP, minS] = data.tempMin.displayDual(metricPrimary);
  const [maxP, maxS] = data.tempMax.displayDual(metricPrimary);
  const dirIndex = ((data.windDeg % 360 + 360) % 360 * 16 / 360) % 16 | 0;

  const uvLabelText =
    data.uvIndex < 3 ? uvLabels[0] :
    data.uvIndex < 6 ? uvLabels[1] :
    data.uvIndex < 8 ? uvLabels[2] :
    data.uvIndex < 11 ? uvLabels[3] :
    uvLabels[4];

  const visKm = (data.visibility / 1000).toFixed(1);
  const visMi = (data.visibility / 1609.34).toFixed(2);

  return (
    <View>
      <View style={{height: 8}} />

      {/* Temperature / Precipitation / Cloud Cover */}
      <View style={[styles.card, {padding: 16 * scale}]}>
        <View style={styles.row}>
          <View style={{flex: 1}}>
            <Text style={[styles.label, {fontSize: 14 * scale, fontFamily: bodyFont}]}>
              {t('detail_temperature', localeTag)}
            </Text>
            <View style={{height: 4}} />
            <DualUnitText
              primary={localizeDigits(tempP)}
              secondary={localizeDigits(tempS)}
              primarySize={20 * scale}
              displayFont={displayFont}
              onPress={onToggleUnits}
            />
            <Text style={[styles.sublabel, {fontSize: 12 * scale, fontFamily: bodyFont}]}>
              {t('feels_like', localeTag)} {localizeDigits(flP)}
            </Text>
          </View>
          <View style={{flex: 1, alignItems: 'flex-end'}}>
            <Text style={[styles.label, {fontSize: 14 * scale, fontFamily: bodyFont, textAlign: 'right'}]}>
              {t('detail_precipitation', localeTag)}
            </Text>
            <View style={{height: 4}} />
            {data.snow ? (
              (() => {
                const [snowP, snowS] = data.snow.displayDual(metricPrimary);
                return (
                  <DualUnitText
                    primary={localizeDigits(`\u2744\uFE0F ${snowP}`)}
                    secondary={localizeDigits(snowS)}
                    primarySize={20 * scale}
                    align="flex-end"
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
                    primary={localizeDigits(`\uD83C\uDF27\uFE0F ${rainP}`)}
                    secondary={localizeDigits(rainS)}
                    primarySize={20 * scale}
                    align="flex-end"
                    displayFont={displayFont}
                    onPress={onToggleUnits}
                  />
                );
              })()
            ) : (
              <Text style={[styles.valueBold, {fontSize: 20 * scale, fontFamily: displayFont, textAlign: 'right'}]}>
                {t('detail_no_precip', localeTag)}
              </Text>
            )}
            <View style={{height: 6}} />
            <Text style={[styles.cloudCover, {fontSize: 12 * scale, fontFamily: bodyFont}]}>
              {t('detail_cloud_cover', localeTag)}: {localizeDigits(`${data.cloudCover}%`)}
            </Text>
          </View>
        </View>
      </View>

      <View style={{height: 16 * scale}} />

      {/* High / Low */}
      <View style={[styles.card, {padding: 16 * scale}]}>
        <View style={[styles.row, {alignItems: 'center'}]}>
          <Text style={[styles.arrow, {fontSize: 28 * scale}]}>{'\u2193'}</Text>
          <View style={{width: 8 * scale}} />
          <DualUnitText primary={localizeDigits(minP)} secondary={localizeDigits(minS)} primarySize={20 * scale} displayFont={displayFont} onPress={onToggleUnits} />
          <View style={{flex: 1}} />
          <DualUnitText primary={localizeDigits(maxP)} secondary={localizeDigits(maxS)} primarySize={20 * scale} align="flex-end" displayFont={displayFont} onPress={onToggleUnits} />
          <View style={{width: 8 * scale}} />
          <Text style={[styles.arrow, {fontSize: 28 * scale}]}>{'\u2191'}</Text>
        </View>
      </View>

      <View style={{height: 16 * scale}} />

      {/* Wind / Wind Gust */}
      <View style={[styles.card, {padding: 16 * scale}]}>
        <View style={styles.row}>
          <View style={{flex: 1}}>
            <DualUnitText primary={localizeDigits(windP)} secondary={localizeDigits(windS)} primarySize={20 * scale} displayFont={displayFont} onPress={onToggleUnits} />
            <Text style={[styles.sublabel, {fontSize: 12 * scale, fontFamily: bodyFont}]}>
              {localizeDigits(`${directions[dirIndex]} (${data.windDeg}\u00B0)`)}
            </Text>
          </View>
          <View style={{flex: 1, alignItems: 'center'}}>
            <Text style={[styles.label, {fontSize: 14 * scale, fontFamily: bodyFont}]}>
              {t('detail_wind', localeTag)}
            </Text>
          </View>
          <View style={{flex: 1, alignItems: 'flex-end'}}>
            {data.windGust && (() => {
              const [gustP, gustS] = data.windGust.displayDual(metricPrimary);
              return (
                <>
                  <DualUnitText primary={localizeDigits(gustP)} secondary={localizeDigits(gustS)} primarySize={20 * scale} align="flex-end" displayFont={displayFont} onPress={onToggleUnits} />
                  <Text style={[styles.sublabel, {fontSize: 12 * scale, fontFamily: bodyFont, textAlign: 'right'}]}>
                    {t('detail_wind_gust', localeTag)}
                  </Text>
                </>
              );
            })()}
          </View>
        </View>
      </View>

      <View style={{height: 16 * scale}} />

      {/* Sunrise / Sunset */}
      <View style={[styles.card, {padding: 16 * scale}]}>
        <View style={[styles.row, {alignItems: 'center'}]}>
          <View style={{flex: 1}}>
            <Text style={[styles.valueBold, {fontSize: 20 * scale, fontFamily: displayFont}]}>
              {localizeDigits(formatTimeFromEpochSeconds(data.sunrise))}
            </Text>
            <Text style={[styles.sublabel, {fontSize: 12 * scale, fontFamily: bodyFont}]}>
              {t('detail_sunrise', localeTag)}
            </Text>
          </View>
          <View style={{flex: 1, alignItems: 'center'}}>
            <Text style={{fontSize: 24 * scale}}>{'\u2600\uFE0F'}</Text>
          </View>
          <View style={{flex: 1, alignItems: 'flex-end'}}>
            <Text style={[styles.valueBold, {fontSize: 20 * scale, fontFamily: displayFont, textAlign: 'right'}]}>
              {localizeDigits(formatTimeFromEpochSeconds(data.sunset))}
            </Text>
            <Text style={[styles.sublabel, {fontSize: 12 * scale, fontFamily: bodyFont, textAlign: 'right'}]}>
              {t('detail_sunset', localeTag)}
            </Text>
          </View>
        </View>
      </View>

      <View style={{height: 16 * scale}} />

      {/* Pressure / Humidity + Dew Point */}
      <View style={[styles.row, {gap: 16 * scale}]}>
        <DetailCard
          title={t('detail_pressure', localeTag)}
          value={localizeDigits(pressP)}
          secondaryValue={localizeDigits(pressS)}
          onToggleUnits={onToggleUnits}
          scale={scale}
          bodyFont={bodyFont}
          displayFont={displayFont}
          style={{flex: 1}}
        />
        <View style={[styles.card, {flex: 1, padding: 16 * scale}]}>
          <Text style={[styles.label, {fontSize: 14 * scale, fontFamily: bodyFont}]}>
            {t('detail_humidity', localeTag)}
          </Text>
          <View style={{height: 8}} />
          <Text style={[styles.valueBold, {fontSize: 20 * scale, fontFamily: displayFont}]}>
            {localizeDigits(`${data.humidity}%`)}
          </Text>
          <View style={{height: 8}} />
          <Text style={[styles.dewLabel, {fontSize: 12 * scale, fontFamily: bodyFont}]}>
            {t('detail_dewpoint', localeTag)}
          </Text>
          <TouchableOpacity onPress={onToggleUnits}>
            <Text>
              <Text style={[styles.dewValue, {fontSize: 13 * scale, fontFamily: displayFont}]}>
                {localizeDigits(dewP)}
              </Text>
              <Text style={[styles.dewSecondary, {fontSize: 12 * scale, fontFamily: displayFont}]}>
                {' '}{localizeDigits(dewS)}
              </Text>
            </Text>
          </TouchableOpacity>
        </View>
      </View>

      <View style={{height: 16 * scale}} />

      {/* UV Index / Visibility */}
      <View style={[styles.row, {gap: 16 * scale}]}>
        <DetailCard
          title={t('detail_uv_index', localeTag)}
          value={localizeDigits(data.uvIndex.toFixed(1))}
          subtitle={uvLabelText}
          scale={scale}
          bodyFont={bodyFont}
          displayFont={displayFont}
          style={{flex: 1}}
        />
        <DetailCard
          title={t('detail_visibility', localeTag)}
          value={localizeDigits(metricPrimary ? `${visKm} km` : `${visMi} mi`)}
          secondaryValue={localizeDigits(metricPrimary ? `${visMi} mi` : `${visKm} km`)}
          onToggleUnits={onToggleUnits}
          scale={scale}
          bodyFont={bodyFont}
          displayFont={displayFont}
          style={{flex: 1}}
        />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: Colors.cardSurfaceTranslucent,
    borderRadius: 16,
  },
  row: {
    flexDirection: 'row',
  },
  label: {
    color: Colors.onSurfaceSecondary,
  },
  sublabel: {
    color: Colors.onSurfaceTertiary,
  },
  valueBold: {
    fontWeight: 'bold',
    color: Colors.onSurface,
  },
  arrow: {
    fontWeight: 'bold',
    color: Colors.onSurfaceSecondary,
  },
  cloudCover: {
    color: Colors.onSurfaceDim,
    textAlign: 'right',
  },
  dewLabel: {
    color: Colors.onSurfaceDim,
  },
  dewValue: {
    fontWeight: 'bold',
    color: Colors.onSurface,
  },
  dewSecondary: {
    color: Colors.onSurfaceTertiary,
  },
});

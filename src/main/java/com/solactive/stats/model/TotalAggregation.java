/*
 * Copyright 2017-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.solactive.stats.model;

import com.solactive.stats.generated.openapi.model.Statistics;
import com.solactive.stats.generated.openapi.model.Tick;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import lombok.NoArgsConstructor;

/**
 * Stores aggregated values for the last WINDOW_SIZE (e.g. 60) seconds. It stores aggregated values for all known instruments and the aggregated
 * values for each instrument.
 */
@NoArgsConstructor
public class TotalAggregation {

  /* The aggregated values for all known instruments. */
  private final AggregatedValues aggregatedValues = new AggregatedValues();
  /* The aggregated values per instrument. */
  private final Map<String, AggregatedValues> instrumentAggregations = new HashMap<>(20_000);

  /* Contains the min and max prices for all partial aggregations (a given epoch second).
   * This set will not have more than WINDOW_SIZE * 2 elements (min & max). */
  private final SortedSet<BigDecimal> allPrices = new TreeSet<>();
  /* Stores the min and max prices for each instrument.
   * Again each entry value (sorted set) will never have more than WINDOW_SIZE * 2 elements (min & max). */
  private final Map<String, SortedSet<BigDecimal>> instrumentPrices = new HashMap<>(20_000);

  /**
   * Updates the aggregated values (for the tick instrument and all known instruments) with the given tick. The passed partial aggregation, is the
   * bucket (epoch second) in which the tick will be aggregated. The min/max price of the partial aggregation will also be stored.
   *
   * @param tick The tick.
   * @param partialAggregation The partial aggregation in which the tick was aggregated.
   */
  public void updateValues(Tick tick, PartialAggregation partialAggregation) {
    aggregatedValues.updateValues(tick.getPrice());
    allPrices.add(partialAggregation.getAggregatedValues().getMinPrice());
    allPrices.add(partialAggregation.getAggregatedValues().getMaxPrice());

    final String instrument = tick.getInstrument();
    instrumentAggregations.computeIfAbsent(instrument, inst -> new AggregatedValues()).updateValues(tick.getPrice());
    Set<BigDecimal> instPrices = instrumentPrices.computeIfAbsent(instrument, inst -> new TreeSet<>());
    instPrices.add(partialAggregation.getAggregatedValuesForInstrument(instrument).getMinPrice());
    instPrices.add(partialAggregation.getAggregatedValuesForInstrument(instrument).getMaxPrice());
  }

  /**
   * Subtracts an expired (older than WINDOW_SIZE (e.g. 60) seconds) partial aggregation from the total aggregations. Subtracts the aggregated values
   * (sum, count) and sets the new min & max price (overall/instrument) from the residual min/max prices.
   *
   * @param partialAggregation The expired partial aggregation.
   */
  public void removePartialAggregation(PartialAggregation partialAggregation) {
    /* Skip an empty partial aggregations. */
    if (partialAggregation.getAggregatedValues().isBlank()) {
      return;
    }

    allPrices.remove(partialAggregation.getAggregatedValues().getMinPrice());
    allPrices.remove(partialAggregation.getAggregatedValues().getMaxPrice());
    BigDecimal minTickPrice = allPrices.isEmpty() ? getZeroBigDecimalScaled() : allPrices.first();
    BigDecimal maxTickPrice = allPrices.isEmpty() ? getZeroBigDecimalScaled() : allPrices.last();
    aggregatedValues.subtractPartialAggregation(partialAggregation.getAggregatedValues(), minTickPrice, maxTickPrice);

    /* Subtract instrument aggregations. */
    partialAggregation.getAggregatedValuesPerInstrument().entrySet().stream().forEach(entry -> {
      String instrumentId = entry.getKey();
      AggregatedValues instAgg = entry.getValue();

      instrumentPrices.get(instrumentId).removeAll(Arrays.asList(instAgg.getMinPrice(), instAgg.getMaxPrice()));
      BigDecimal minTickPriceInstrument = instrumentPrices.get(instrumentId).isEmpty() ?
          getZeroBigDecimalScaled() : instrumentPrices.get(instrumentId).first();
      BigDecimal maxTickPriceInstrument = instrumentPrices.get(instrumentId).isEmpty() ?
          getZeroBigDecimalScaled() : instrumentPrices.get(instrumentId).last();
      instrumentAggregations.get(instrumentId).subtractPartialAggregation(instAgg, minTickPriceInstrument, maxTickPriceInstrument);

      if (instrumentPrices.get(instrumentId).isEmpty()) {
        instrumentPrices.remove(instrumentId);
      }
      if (instrumentAggregations.get(instrumentId).isBlank()) {
        instrumentAggregations.remove(instrumentId);
      }
    });
  }

  /**
   * Creates a {@link Statistics} snapshot from the current aggregated values.
   *
   * @return The current statistics.
   */
  public Statistics getStatistics() {
    return new Statistics()
        .avg(aggregatedValues.getAvgPrice())
        .max(aggregatedValues.getMaxPrice())
        .min(aggregatedValues.getMinPrice())
        .count(aggregatedValues.getCount());
  }

  /**
   * Creates a {@link Statistics} snapshot for a given instrument.
   *
   * @param instrumentId The instrument identifier.
   * @return The current statistics for the given instrument.
   */
  public Statistics getStatisticsForInstrument(final String instrumentId) {
    if (instrumentAggregations.containsKey(instrumentId)) {
      AggregatedValues instAgg = instrumentAggregations.get(instrumentId);
      return new Statistics()
          .avg(instAgg.getAvgPrice())
          .max(instAgg.getMaxPrice())
          .min(instAgg.getMinPrice())
          .count(instAgg.getCount());
    }
    return new Statistics().avg(getZeroBigDecimalScaled()).max(getZeroBigDecimalScaled()).min(getZeroBigDecimalScaled()).count(0L);
  }

  private BigDecimal getZeroBigDecimalScaled() {
    return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
  }
}

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

import com.solactive.stats.generated.openapi.model.Tick;
import java.util.HashMap;
import java.util.Map;
import lombok.NoArgsConstructor;

/**
 * A partial aggregation stores aggregated values for a given epoch second (bucket).
 */
@NoArgsConstructor
public class PartialAggregation {

  /* The aggregated values for all known instruments. */
  private final AggregatedValues aggregatedValues = new AggregatedValues();
  /* The aggregated values per instrument. */
  private final Map<String, AggregatedValues> instrumentAggregations = new HashMap<>(20_000);

  /**
   * Updates the aggregated values (for the tick instrument and all known instruments) with the given tick.
   *
   * @param tick The tick.
   */
  public void updateValues(final Tick tick) {
    aggregatedValues.updateValues(tick.getPrice());
    instrumentAggregations.computeIfAbsent(tick.getInstrument(), instrument -> new AggregatedValues()).updateValues(tick.getPrice());
  }

  /**
   * Returns the aggregated values for the current epoch second.
   *
   * @return The aggregated values (min, max, sum, count).
   */
  AggregatedValues getAggregatedValues() {
    return aggregatedValues;
  }

  /**
   * Returns a per instrument aggregated values map.
   *
   * @return Map of aggregated values for each instrument.
   */
  Map<String, AggregatedValues> getAggregatedValuesPerInstrument() {
    return instrumentAggregations;
  }

  /**
   * Returns the aggregated values for the given instrument.
   *
   * @param instrumentId The instrument identifier.
   * @return The aggregated values (min, max, sum, count).
   */
  AggregatedValues getAggregatedValuesForInstrument(String instrumentId) {
    return instrumentAggregations.getOrDefault(instrumentId, new AggregatedValues());
  }
}

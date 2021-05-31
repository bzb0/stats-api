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
package com.solactive.stats.service;

import com.solactive.stats.generated.openapi.model.Statistics;
import com.solactive.stats.generated.openapi.model.Tick;
import com.solactive.stats.model.PartialAggregation;
import com.solactive.stats.model.TotalAggregation;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.validation.ClockProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Aggregates ticks for the last {@code WINDOW_SIZE} seconds and provides statistics for all instruments and for each instrument individually.
 *
 * The sliding time window is implemented as sorted map of epoch seconds to partial aggregations (buckets). Every bucket aggregates all ticks that
 * have their timestamp in a certain epoch second.
 *
 * The sliding window doesn't store each tick individually and incrementally updates the partial aggregations (buckets) and the total aggregation.
 * When the oldest bucket is removed/evicted, the aggregated values of that bucket are subtracted from the total aggregation. This sliding window
 * aggregation algorithm is called Subtract-on-Evict.
 *
 * As all values are pre-aggregated the time for retrieving a {@link Statistics} object for all instruments or a given instrument is constant O(1).
 * The space requirement of the sliding window will be O(WINDOW_SIZE) for the partial aggregations and O(1) for the total aggregation.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class InstrumentAggregator {

  private final Clock clock;
  private final int windowSizeSeconds;
  private final TotalAggregation totalAggregation = new TotalAggregation();
  private final SortedMap<Long, PartialAggregation> partialAggregations = new TreeMap<>();

  @Autowired
  public InstrumentAggregator(ClockProvider clockProvider, @Value("${slidingWindow.sizeInSeconds}") int windowSizeSeconds) {
    this.clock = clockProvider.getClock();
    this.windowSizeSeconds = windowSizeSeconds;
  }

  /**
   * Checks if the tick is valid and records it in the total and partial aggregations (epoch second bucket).
   *
   * @param tick The tick to be recorded.
   */
  synchronized void record(Tick tick) {
    if (!isTickValid(tick)) {
      return;
    }
    moveWindow();
    long tickEpochSecond = Instant.ofEpochMilli(tick.getTimestamp()).getEpochSecond();
    PartialAggregation partialAggregation = partialAggregations.computeIfAbsent(tickEpochSecond, epochSecond -> new PartialAggregation());
    partialAggregation.updateValues(tick);
    totalAggregation.updateValues(tick, partialAggregation);
  }

  /**
   * Returns a {@link Statistics} for all known instruments for the last WINDOW_SIZE (e.g. 60) seconds.
   *
   * @return The statistics.
   */
  synchronized Statistics getStatistics() {
    moveWindow();
    return totalAggregation.getStatistics();
  }

  /**
   * Returns a {@link Statistics} for the given instrument for the last WINDOW_SIZE (e.g. 60) seconds.
   *
   * @param instrumentId The instrument identifier.
   * @return The statistics for the given instrument.
   */
  synchronized Statistics getStatisticsForInstrument(String instrumentId) {
    moveWindow();
    return totalAggregation.getStatisticsForInstrument(instrumentId);
  }

  /**
   * Moves the sliding window to the current epoch second and removes all partial aggregations that are not inside the sliding window. This method is
   * called by the {@link SlidingWindowService} every second, because if there are no new ticks for a certain time the list of partial aggregations
   * will not be up-to-date and will contain expired partial aggregations.
   */
  synchronized void moveWindow() {
    if (partialAggregations.isEmpty()) {
      return;
    }
    long currentEpochSecond = clock.instant().getEpochSecond();
    long minusWindow = currentEpochSecond - windowSizeSeconds;
    long oldEpochSecond = partialAggregations.firstKey();
    while (oldEpochSecond < minusWindow) {
      if (partialAggregations.containsKey(oldEpochSecond)) {
        totalAggregation.removePartialAggregation(partialAggregations.remove(oldEpochSecond));
      }
      oldEpochSecond++;
    }
  }

  /**
   * Checks if the tick timestamp is older than WINDOW_SIZE (e.g. 60) seconds.
   *
   * @param tick The tick to be stored.
   * @return {@code true} if the tick is not older than WINDOW_SIZE (e.g. 60) seconds, {@code false} otherwise.
   */
  public boolean isTickValid(Tick tick) {
    Instant maxValidityInstant = clock.instant().minus(Duration.ofSeconds(windowSizeSeconds));
    Instant tickInstant = Instant.ofEpochMilli(tick.getTimestamp());
    return !tickInstant.isBefore(maxValidityInstant);
  }
}

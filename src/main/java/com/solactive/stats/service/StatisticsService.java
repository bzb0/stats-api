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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * The {@link StatisticsService} can be seen as a service facade for storing ticks and retrieving aggregated statistics in the underlying {@link
 * InstrumentAggregator}. This service is implicitly thread-safe as it doesn't have an encapsulated state.
 */
@Service
public class StatisticsService {

  private final InstrumentAggregator instrumentAggregator;

  @Autowired
  public StatisticsService(InstrumentAggregator instrumentAggregator) {
    this.instrumentAggregator = instrumentAggregator;
  }

  /**
   * Stores the tick (price change for a given financial instrument) and updates the aggregated statistics.
   *
   * Note:  assumes that the provided tick is valid (not older than 60 seconds),
   *
   * @param tick The financial instrument for which the price change will be stored.
   */
  public void storeTick(Tick tick) {
    instrumentAggregator.record(tick);
  }

  /**
   * Returns aggregated statistics for all ticks across all instruments.
   *
   * @return The aggregated statistics.
   */
  public Statistics getStatistics() {
    return instrumentAggregator.getStatistics();
  }

  /**
   * Returns aggregated statistics for the given instrument identifier.
   *
   * @param instrumentId The instrument identifier.
   * @return The instrument aggregated statistics.
   */
  public Statistics getStatisticsForInstrument(String instrumentId) {
    return instrumentAggregator.getStatisticsForInstrument(instrumentId);
  }

  /**
   * Checks if the tick timestamp is older than 60 seconds.
   *
   * @param tick The tick to be stored.
   * @return {@code true} if the tick is not older than 60 seconds, {@code false} otherwise.
   */
  public boolean isTickValid(Tick tick) {
    return instrumentAggregator.isTickValid(tick);
  }
}

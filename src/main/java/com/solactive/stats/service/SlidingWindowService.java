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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * The sole purpose of this service is to move the sliding window of the {@link InstrumentAggregator every second}.
 */
@Service
public class SlidingWindowService {

  private final InstrumentAggregator instrumentAggregator;

  @Autowired
  public SlidingWindowService(InstrumentAggregator instrumentAggregator) {
    this.instrumentAggregator = instrumentAggregator;
  }

  /**
   * Moves the {@link InstrumentAggregator}'s sliding window every second. By doing this the sliding window will not contain expired partial
   * aggregations.
   */
  @Scheduled(fixedRate = 1000L)
  public void moveWindow() {
    instrumentAggregator.moveWindow();
  }
}

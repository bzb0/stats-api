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
package com.solactive.stats.controller;

import com.solactive.stats.generated.openapi.api.StatisticsApi;
import com.solactive.stats.generated.openapi.model.Statistics;
import com.solactive.stats.generated.openapi.model.Tick;
import com.solactive.stats.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * A REST controller for the 'ticks' and 'statistics' resource. The 'ticks' resource could be placed in a separate controller, but for brevity it was
 * implemented here.
 */
@RestController
public class StatisticsController implements StatisticsApi {

  private final StatisticsService statisticsService;

  @Autowired
  public StatisticsController(StatisticsService statisticsService) {
    this.statisticsService = statisticsService;
  }

  @Override
  public ResponseEntity<Void> storeTick(Tick tick) {
    if (statisticsService.isTickValid(tick)) {
      statisticsService.storeTick(tick);
      return ResponseEntity.status(HttpStatus.CREATED).build();
    }
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Statistics> getStatistics() {
    return ResponseEntity.ok(statisticsService.getStatistics());
  }

  @Override
  public ResponseEntity<Statistics> getStatisticsForInstrument(String instrumentId) {
    return ResponseEntity.ok(statisticsService.getStatisticsForInstrument(instrumentId));
  }
}

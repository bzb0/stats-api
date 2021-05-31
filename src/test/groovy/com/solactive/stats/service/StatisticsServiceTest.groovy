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
package com.solactive.stats.service

import com.solactive.stats.generated.openapi.model.Statistics
import com.solactive.stats.generated.openapi.model.Tick
import spock.lang.Specification

class StatisticsServiceTest extends Specification {

  def instrumentAggregatorMock = Mock(InstrumentAggregator)
  def sut = new StatisticsService(instrumentAggregatorMock)

  def "StatisticsService constructor initializes StatisticsController correctly"() {
    when:
    def result = new StatisticsService(instrumentAggregatorMock)

    then:
    result.instrumentAggregator == instrumentAggregatorMock
  }

  def "storeTick stores a tick in the aggregation service"() {
    given:
    def tick = new Tick()

    when:
    sut.storeTick(tick)

    then:
    1 * instrumentAggregatorMock.record(tick)
  }

  def "getStatistics returns a Statistics object for all aggregated values"() {
    given:
    def stats = new Statistics()

    when:
    def result = sut.getStatistics()

    then:
    1 * instrumentAggregatorMock.getStatistics() >> stats
    result == stats
  }

  def "getStatisticsForInstrument returns Statistics object for a given instrument identifier"() {
    given:
    def instrumentId = "ABC"
    def instrumentStats = new Statistics()

    when:
    def result = sut.getStatisticsForInstrument(instrumentId)

    then:
    1 * instrumentAggregatorMock.getStatisticsForInstrument(instrumentId) >> instrumentStats
    result == instrumentStats
  }

  def "isTickValid checks if a tick is valid"() {
    given:
    def tick = new Tick()

    when:
    def result = sut.isTickValid(tick)

    then:
    1 * instrumentAggregatorMock.isTickValid(tick) >> true
    result == true
  }
}

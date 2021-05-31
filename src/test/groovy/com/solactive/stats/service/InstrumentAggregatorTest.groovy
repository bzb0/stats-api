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
import com.statemachinesystems.mockclock.MockClock
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import javax.validation.ClockProvider
import java.time.ZoneId

class InstrumentAggregatorTest extends Specification {

  static final WINDOW_SIZE_SECONDS = 60

  @Shared
  def mockClock = MockClock.at(2020, 5, 30, 12, 0, 0, ZoneId.of("UTC"))
  def clockProviderMock = Mock(ClockProvider) {
    getClock() >> mockClock
  }
  def sut = new InstrumentAggregator(clockProviderMock, WINDOW_SIZE_SECONDS)

  def "AggregationService constructor adds 60 epoch seconds buckets"() {
    given:
    def mockClock = MockClock.systemUTC()
    def clockProviderMock = Mock(ClockProvider) {
      getClock() >> mockClock
    }

    when:
    def tickAggregator = new InstrumentAggregator(clockProviderMock, WINDOW_SIZE_SECONDS)

    then:
    tickAggregator.windowSizeSeconds == WINDOW_SIZE_SECONDS
    tickAggregator.clock == mockClock
  }

  def "moveWindow on empty aggregations is a noop"() {
    when:
    sut.moveWindow()

    then:
    noExceptionThrown()
  }

  @Unroll
  def "isTickValid checks if a tick is older than 60 seconds"() {
    when:
    def result = sut.isTickValid(tick)

    then:
    result == expectedResult

    where:
    tick                                                                                                                      || expectedResult
    new Tick().instrument("ABC").price(new BigDecimal("0.00")).timestamp(mockClock.instant().minusSeconds(5).toEpochMilli())  || true
    new Tick().instrument("ABC").price(new BigDecimal("0.00")).timestamp(mockClock.instant().minusSeconds(65).toEpochMilli()) || false
  }

  def "record updates the partial and total aggregations"() {
    given:
    def instrument = "ABC"
    def price = new BigDecimal("50.55")
    def timestamp = mockClock.instant().minusSeconds(5).toEpochMilli()
    def tick = new Tick().instrument(instrument).price(price).timestamp(timestamp)

    when:
    sut.record(tick)

    then:
    def totalStats = sut.getStatistics()
    totalStats.getAvg() == price
    totalStats.getCount() == 1
    totalStats.getMax() == price
    totalStats.getMin() == price

    def instrumentStats = sut.getStatisticsForInstrument(instrument)
    instrumentStats.getAvg() == price
    instrumentStats.getCount() == 1
    instrumentStats.getMax() == price
    instrumentStats.getMin() == price
  }

  def "record ignores ticks older than 60 seconds"() {
    given:
    def emptyStats = new Statistics().avg(new BigDecimal("0.00")).min(new BigDecimal("0.00")).max(new BigDecimal("0.00")).count(0L)
    def timestamp = mockClock.instant().minusSeconds(61).toEpochMilli()
    def tick = new Tick().instrument("ABC").price(new BigDecimal("50.12")).timestamp(timestamp)

    when:
    sut.record(tick)

    then:
    sut.getStatistics() == emptyStats
  }

  def "record for several ticks updates the total and instrument aggregations"() {
    given:
    def timestamp1 = mockClock.instant().minusSeconds(5).toEpochMilli()
    def timestamp2 = mockClock.instant().minusSeconds(3).toEpochMilli()
    def tick1 = new Tick().instrument("ABC").price(new BigDecimal("11.58")).timestamp(timestamp1)
    def tick2 = new Tick().instrument("DEF").price(new BigDecimal("20.13")).timestamp(timestamp2)

    when:
    sut.record(tick1)
    sut.record(tick2)

    then:
    def totalStats = sut.getStatistics()
    totalStats.getAvg() == new BigDecimal("15.86")
    totalStats.getCount() == 2
    totalStats.getMax() == tick2.getPrice()
    totalStats.getMin() == tick1.getPrice()

    def instrumentStats = sut.getStatisticsForInstrument(tick1.getInstrument())
    instrumentStats.getAvg() == tick1.getPrice()
    instrumentStats.getCount() == 1
    instrumentStats.getMax() == tick1.getPrice()
    instrumentStats.getMin() == tick1.getPrice()
  }

  def "getStatistics returns an empty Statistics object if the stored tick is not anymore in the last 60 seconds"() {
    given:
    def emptyStats = new Statistics().avg(new BigDecimal("0.00")).min(new BigDecimal("0.00")).max(new BigDecimal("0.00")).count(0L)
    def sut = new InstrumentAggregator(clockProviderMock, WINDOW_SIZE_SECONDS)
    def timestamp = mockClock.instant().minusSeconds(50).toEpochMilli()
    def tick1 = new Tick().instrument("ABC").price(new BigDecimal("50.22")).timestamp(timestamp)

    and:
    sut.record(tick1)
    mockClock.advanceBySeconds(11)

    when:
    def result = sut.getStatistics()

    then:
    result == emptyStats
  }

  def "getStatisticsForInstrument returns an empty Statistics object if the stored tick is not anymore in the last 60 seconds"() {
    given:
    def instrumentId = "ABC"
    def emptyStats = new Statistics().avg(new BigDecimal("0.00")).min(new BigDecimal("0.00")).max(new BigDecimal("0.00")).count(0L)
    def sut = new InstrumentAggregator(clockProviderMock, WINDOW_SIZE_SECONDS)
    def timestamp = mockClock.instant().minusSeconds(50).toEpochMilli()
    def tick1 = new Tick().instrument(instrumentId).price(10.0).timestamp(timestamp)

    and:
    sut.record(tick1)
    mockClock.advanceBySeconds(11)

    when:
    def result = sut.getStatisticsForInstrument(instrumentId)

    then:
    result == emptyStats
  }

  def "getStatistics returns correct aggregations for different clock advancements"() {
    given:
    def mockClock = MockClock.at(2020, 5, 30, 12, 0, 0, ZoneId.of("UTC"))
    def clockProviderMock = Mock(ClockProvider) {
      getClock() >> mockClock
    }
    def sut = new InstrumentAggregator(clockProviderMock, WINDOW_SIZE_SECONDS)

    def emptyStats = new Statistics().avg(new BigDecimal("0.00")).min(new BigDecimal("0.00")).max(new BigDecimal("0.00")).count(0L)
    def timestamp1 = mockClock.instant().toEpochMilli()
    def timestamp2 = mockClock.instant().minusSeconds(10).toEpochMilli()
    def timestamp3 = mockClock.instant().minusSeconds(20).toEpochMilli()
    def timestamp4 = mockClock.instant().minusSeconds(30).toEpochMilli()
    def ticks = [new Tick().instrument("ABC").price(new BigDecimal("15.00")).timestamp(timestamp1),
                 new Tick().instrument("XYZ").price(new BigDecimal("55.00")).timestamp(timestamp2),
                 new Tick().instrument("DEF").price(new BigDecimal("65.00")).timestamp(timestamp3),
                 new Tick().instrument("ABC").price(new BigDecimal("105.00")).timestamp(timestamp4)]

    and:
    ticks.each {
      tick ->
        sut.record(tick)
        mockClock.advanceBySeconds(5)
    }

    when:
    def totalStats = sut.getStatistics()
    def instrumentStats = sut.getStatisticsForInstrument("ABC")

    then:
    totalStats == new Statistics().avg(new BigDecimal("60.00")).min(new BigDecimal("15.00")).max(new BigDecimal("105.00")).count(4L)
    instrumentStats == new Statistics().avg(new BigDecimal("60.00")).min(new BigDecimal("15.00")).max(new BigDecimal("105.00")).count(2L)

    when:
    mockClock.advanceBySeconds(11)

    then:
    def totalStats1 = sut.getStatistics()
    totalStats1 == new Statistics().avg(new BigDecimal("45.00")).min(new BigDecimal("15.00")).max(new BigDecimal("65.00")).count(3L)
    def instrumentStats1 = sut.getStatisticsForInstrument("ABC")
    instrumentStats1 == new Statistics().avg(new BigDecimal("15.00")).min(new BigDecimal("15.00")).max(new BigDecimal("15.00")).count(1L)

    when:
    mockClock.advanceBySeconds(11)

    then:
    def totalStats2 = sut.getStatistics()
    totalStats2 == new Statistics().avg(new BigDecimal("35.00")).min(new BigDecimal("15.00")).max(new BigDecimal("55.00")).count(2L)
    def instrumentStats2 = sut.getStatisticsForInstrument("DEF")
    instrumentStats2 == emptyStats

    when:
    mockClock.advanceBySeconds(19)

    then:
    sut.getStatistics() == emptyStats
  }
}

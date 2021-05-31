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
package com.solactive.stats.controller

import com.solactive.stats.generated.openapi.model.Statistics
import com.solactive.stats.generated.openapi.model.Tick
import com.solactive.stats.service.StatisticsService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Specification

class StatisticsControllerTest extends Specification {

  def statisticsServiceMock = Mock(StatisticsService)
  def sut = new StatisticsController(statisticsServiceMock)

  def "StatisticsController constructor initializes StatisticsController correctly"() {
    when:
    def result = new StatisticsController(statisticsServiceMock)

    then:
    result.statisticsService == statisticsServiceMock
  }

  def "storeTick stores a tick and returns HTTP 201 ResponseEntity without body"() {
    given:
    def tick = new Tick()
    def expectedResponse = ResponseEntity.status(HttpStatus.CREATED).build()

    when:
    def result = sut.storeTick(tick)

    then:
    1 * statisticsServiceMock.isTickValid(tick) >> true
    1 * statisticsServiceMock.storeTick(tick)
    result == expectedResponse
  }

  def "storeTick rejects ticks older than 60 seconds and returns HTTP 204 ResponseEntity without body"() {
    given:
    def tick = new Tick()
    def expectedResponse = ResponseEntity.noContent().build()

    when:
    def result = sut.storeTick(tick)

    then:
    1 * statisticsServiceMock.isTickValid(tick) >> false
    result == expectedResponse
  }


  def "getStatistics returns a HTTP 200 ResponseEntity with a Statistics object"() {
    given:
    def stats = new Statistics()
    def expectedResponse = ResponseEntity.ok(stats)

    when:
    def result = sut.getStatistics()

    then:
    1 * statisticsServiceMock.getStatistics() >> stats
    result == expectedResponse
  }

  def "getStatisticsForInstrument returns a HTTP 200 ResponseEntity with a Statistics object for a given instrument identifier"() {
    given:
    def instrumentId = "ABC"
    def instrumentStats = new Statistics()
    def expectedResponse = ResponseEntity.ok(instrumentStats)

    when:
    def result = sut.getStatisticsForInstrument(instrumentId)

    then:
    1 * statisticsServiceMock.getStatisticsForInstrument(instrumentId) >> instrumentStats
    result == expectedResponse
  }
}

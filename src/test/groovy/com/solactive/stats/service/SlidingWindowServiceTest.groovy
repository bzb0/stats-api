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

import spock.lang.Specification

class SlidingWindowServiceTest extends Specification {

  def instrumentAggregatorMock = Mock(InstrumentAggregator)
  def sut = new SlidingWindowService(instrumentAggregatorMock)

  def "SlidingWindowService constructor initializes SlidingWindowService correctly"() {
    when:
    def result = new SlidingWindowService(instrumentAggregatorMock)

    then:
    result.instrumentAggregator == instrumentAggregatorMock
  }

  def "moveWindow moves the InstrumentAggregator sliding window"() {
    when:
    sut.moveWindow()

    then:
    1 * instrumentAggregatorMock.moveWindow()
  }
}

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
package com.solactive.stats.model

import com.solactive.stats.generated.openapi.model.Tick
import spock.lang.Specification

class PartialAggregationTest extends Specification {

  def "PartialAggregation constructor creates an empty PartialAggregation with given epoch second"() {
    when:
    def result = new PartialAggregation()

    then:
    result.getAggregatedValues().isBlank()
    result.getAggregatedValuesPerInstrument().isEmpty()
  }

  def "updateValues updates total/instrument aggregated values"() {
    given:
    def epochSecond = 1590839940L
    def sut = new PartialAggregation()
    def tick = new Tick().instrument("ABC").price(new BigDecimal("50.55")).timestamp(epochSecond)

    when:
    sut.updateValues(tick)

    then:
    def aggTicks = sut.getAggregatedValues()
    aggTicks.getMaxPrice() == tick.getPrice()
    aggTicks.getMinPrice() == tick.getPrice()
    aggTicks.getPriceSum() == tick.getPrice()
    aggTicks.getCount() == 1

    def instAggTicks = sut.getAggregatedValuesForInstrument(tick.getInstrument())
    instAggTicks.getMaxPrice() == tick.getPrice()
    instAggTicks.getMinPrice() == tick.getPrice()
    instAggTicks.getPriceSum() == tick.getPrice()
    instAggTicks.getCount() == 1
  }

  def "updateValues for several ticks updates total/instrument aggregated values"() {
    given:
    def epochSecond = 1590839940L
    def sut = new PartialAggregation()
    def ticks = [new Tick().instrument("ABC").price(new BigDecimal("25.51")).timestamp(epochSecond),
                 new Tick().instrument("ABC").price(new BigDecimal("51.23")).timestamp(epochSecond),
                 new Tick().instrument("DEF").price(new BigDecimal("22.44")).timestamp(epochSecond),
                 new Tick().instrument("EFG").price(new BigDecimal("33.61")).timestamp(epochSecond)]

    when:
    ticks.each {
      tick -> sut.updateValues(tick)
    }

    then:
    def aggTicks = sut.getAggregatedValues()
    aggTicks.getMinPrice() == ticks.stream().map(t -> t.getPrice()).max(Comparator.reverseOrder()).get()
    aggTicks.getMaxPrice() == ticks.stream().map(t -> t.getPrice()).max(Comparator.naturalOrder()).get()
    aggTicks.getPriceSum() == ticks.stream().map(t -> t.getPrice()).reduce(BigDecimal.ZERO, BigDecimal::add)
    aggTicks.getCount() == ticks.size()

    def instAggTicks = sut.getAggregatedValuesForInstrument("ABC")
    instAggTicks.getMinPrice() == ticks.get(0).getPrice()
    instAggTicks.getMaxPrice() == ticks.get(1).getPrice()
    instAggTicks.getPriceSum() == ticks.get(0).getPrice() + ticks.get(1).getPrice()
    instAggTicks.getCount() == 2
  }

  def "getAggregatedValuesForInstrument returns empty aggregated values initialized with zeros for an unknown instrument"() {
    given:
    def sut = new PartialAggregation()

    when:
    def result = sut.getAggregatedValuesForInstrument("UNKNOWN")

    then:
    result.getMinPrice() == BigDecimal.ZERO
    result.getMaxPrice() ==BigDecimal.ZERO
    result.getCount() == 0L
    result.getPriceSum() == BigDecimal.ZERO
  }
}

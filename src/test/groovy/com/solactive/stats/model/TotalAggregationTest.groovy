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

import com.solactive.stats.generated.openapi.model.Statistics
import com.solactive.stats.generated.openapi.model.Tick
import spock.lang.Specification

class TotalAggregationTest extends Specification {

  def "TotalAggregation constructor creates a TotalAggregation with default values"() {
    given:
    def emptyStats = new Statistics().avg(new BigDecimal("0.00")).min(new BigDecimal("0.00")).max(new BigDecimal("0.00")).count(0L)

    when:
    def result = new TotalAggregation()

    then:
    result.allPrices.isEmpty()
    result.instrumentAggregations.isEmpty()
    result.getStatistics() == emptyStats
    result.getStatisticsForInstrument("UNKNOWN") == emptyStats
  }

  def "updateValues updates aggregated values and stores the new min/max price"() {
    given:
    def epochSecond = 1590839940L
    def sut = new TotalAggregation()
    def tick = new Tick().instrument("ABC").price(new BigDecimal("50.55")).timestamp(epochSecond)
    def partialTickAggregation = createPartialTickAggregation([tick])
    def expectedStats = new Statistics().avg(tick.getPrice()).min(tick.getPrice()).max(tick.getPrice()).count(1)

    when:
    sut.updateValues(tick, partialTickAggregation)

    then:
    sut.allPrices.size() == 1
    sut.allPrices.first() == tick.getPrice()
    sut.allPrices.last() == tick.getPrice()
    sut.getStatistics() == expectedStats
    sut.getStatisticsForInstrument(tick.getInstrument()) == expectedStats
  }

  def "updateValues for several ticks updates the total/instrument aggregated values"() {
    given:
    def epochSecond1 = 1590839940L
    def epochSecond2 = 1590839941L
    def sut = new TotalAggregation()
    def ticksBucket1 = [new Tick().instrument("ABC").price(new BigDecimal("25.11")).timestamp(epochSecond1),
                        new Tick().instrument("DEF").price(new BigDecimal("150.22")).timestamp(epochSecond1)]
    def ticksBucket2 = [new Tick().instrument("ABC").price(new BigDecimal("60.32")).timestamp(epochSecond2),
                        new Tick().instrument("EFG").price(new BigDecimal("11.55")).timestamp(epochSecond2)]
    def partialAgg1 = createPartialTickAggregation(ticksBucket1)
    def partialAgg2 = createPartialTickAggregation(ticksBucket2)
    def allTicks = ticksBucket1 + ticksBucket2

    when:
    ticksBucket1.each {
      tick -> sut.updateValues(tick, partialAgg1)
    }
    ticksBucket2.each {
      tick -> sut.updateValues(tick, partialAgg2)
    }

    then:
    def totalStats = sut.getStatistics()
    totalStats.getMin() == allTicks.stream().map(t -> t.getPrice()).min(Comparator.naturalOrder()).get()
    totalStats.getMax() == allTicks.stream().map(t -> t.getPrice()).max(Comparator.naturalOrder()).get()
    totalStats.getAvg() == new BigDecimal("61.80")
    totalStats.getCount() == allTicks.size()

    def instrumentStats = sut.getStatisticsForInstrument("ABC")
    instrumentStats.getMin() == ticksBucket1.get(0).getPrice()
    instrumentStats.getMax() == ticksBucket2.get(0).getPrice()
    instrumentStats.getAvg() == new BigDecimal("42.72")
    instrumentStats.getCount() == 2

    sut.allPrices.size() == 4
    sut.allPrices.size() == 4
    sut.allPrices.first() == ticksBucket2.get(1).getPrice()
    sut.allPrices.last() == ticksBucket1.get(1).getPrice()
  }

  def "removePartialAggregation for multiple PartialAggregation updates the total/instrument aggregated values"() {
    given:
    def sut = new TotalAggregation()
    def epochSecond1 = 1590839940L
    def epochSecond2 = 1590839941L
    def epochSecond3 = 1590839942L
    def ticksBucket1 = [new Tick().instrument("ABC").price(new BigDecimal("500.12")).timestamp(epochSecond1),
                        new Tick().instrument("DEF").price(new BigDecimal("30.43")).timestamp(epochSecond1),
                        new Tick().instrument("ZXY").price(new BigDecimal("2.05")).timestamp(epochSecond1)]
    def ticksBucket2 = [new Tick().instrument("ABC").price(new BigDecimal("150.44")).timestamp(epochSecond2),
                        new Tick().instrument("DEF").price(new BigDecimal("6.82")).timestamp(epochSecond2),
                        new Tick().instrument("ZXY").price(new BigDecimal("60.55")).timestamp(epochSecond2)]
    def ticksBucket3 = [new Tick().instrument("DEF").price(new BigDecimal("450.33")).timestamp(epochSecond3),
                        new Tick().instrument("ZXY").price(new BigDecimal("11.66")).timestamp(epochSecond3),
                        new Tick().instrument("ABC").price(new BigDecimal("3.22")).timestamp(epochSecond3)]
    def partialAgg1 = createPartialTickAggregation(ticksBucket1)
    def partialAgg2 = createPartialTickAggregation(ticksBucket2)
    def partialAgg3 = createPartialTickAggregation(ticksBucket3)
    def ticksBucket2And3 = ticksBucket2 + ticksBucket3

    and:
    ticksBucket1.each {
      tick -> sut.updateValues(tick, partialAgg1)
    }
    ticksBucket2.each {
      tick -> sut.updateValues(tick, partialAgg2)
    }
    ticksBucket3.each {
      tick -> sut.updateValues(tick, partialAgg3)
    }

    when:
    sut.removePartialAggregation(partialAgg1)

    then:
    sut.allPrices.size() == 4
    sut.allPrices.first() == ticksBucket2And3.stream().map(t -> t.getPrice()).min(Comparator.naturalOrder()).get()
    sut.allPrices.last() == ticksBucket2And3.stream().map(t -> t.getPrice()).max(Comparator.naturalOrder()).get()

    def totalStats = sut.getStatistics()
    totalStats.getMin() == ticksBucket2And3.stream().map(t -> t.getPrice()).min(Comparator.naturalOrder()).get()
    totalStats.getMax() == ticksBucket2And3.stream().map(t -> t.getPrice()).max(Comparator.naturalOrder()).get()
    totalStats.getAvg() == new BigDecimal("113.84")
    totalStats.getCount() == ticksBucket2And3.size()

    def abcInst = "ABC"
    def abcInstrumentStats = sut.getStatisticsForInstrument(abcInst)
    abcInstrumentStats.getMin() == ticksBucket2And3.stream().filter(t -> t.getInstrument() == abcInst)
        .map(t -> t.getPrice()).min(Comparator.naturalOrder()).get()
    abcInstrumentStats.getMax() == ticksBucket2And3.stream().filter(t -> t.getInstrument() == abcInst)
        .map(t -> t.getPrice()).max(Comparator.naturalOrder()).get()
    abcInstrumentStats.getAvg() == new BigDecimal("76.83")
    abcInstrumentStats.getCount() == ticksBucket2And3.stream().filter(t -> t.getInstrument() == abcInst).count()


    when:
    sut.removePartialAggregation(partialAgg2)

    then:
    sut.allPrices.size() == 2
    sut.allPrices.first() == ticksBucket3.stream().map(t -> t.getPrice()).min(Comparator.naturalOrder()).get()
    sut.allPrices.last() == ticksBucket3.stream().map(t -> t.getPrice()).max(Comparator.naturalOrder()).get()

    def totalStats2 = sut.getStatistics()
    totalStats2.getMin() == ticksBucket3.stream().map(t -> t.getPrice()).min(Comparator.naturalOrder()).get()
    totalStats2.getMax() == ticksBucket3.stream().map(t -> t.getPrice()).max(Comparator.naturalOrder()).get()
    totalStats2.getAvg() == new BigDecimal("155.07")
    totalStats2.getCount() == ticksBucket3.size()

    def zxyInst = "ZXY"
    def zxyInstrumentStats = sut.getStatisticsForInstrument(zxyInst)
    zxyInstrumentStats.getMin() == ticksBucket3.stream().filter(t -> t.getInstrument() == zxyInst)
        .map(t -> t.getPrice()).min(Comparator.naturalOrder()).get()
    zxyInstrumentStats.getMax() == ticksBucket3.stream().filter(t -> t.getInstrument() == zxyInst)
        .map(t -> t.getPrice()).max(Comparator.naturalOrder()).get()
    zxyInstrumentStats.getAvg() == new BigDecimal("11.66")
    zxyInstrumentStats.getCount() == ticksBucket3.stream().filter(t -> t.getInstrument() == zxyInst).count()
  }

  def "updateValues & removePartialAggregation return empty statistics"() {
    given:
    def epochSecond1 = 1590839940L
    def epochSecond2 = 1590839941L
    def sut = new TotalAggregation()
    def ticksBucket1 = [new Tick().instrument("ABC").price(new BigDecimal("25.11")).timestamp(epochSecond1),
                        new Tick().instrument("DEF").price(new BigDecimal("150.32")).timestamp(epochSecond1)]
    def ticksBucket2 = [new Tick().instrument("ABC").price(new BigDecimal("60.18")).timestamp(epochSecond2),
                        new Tick().instrument("EFG").price(new BigDecimal("11.43")).timestamp(epochSecond2)]
    def partialAgg1 = createPartialTickAggregation(ticksBucket1)
    def partialAgg2 = createPartialTickAggregation(ticksBucket2)
    def emptyStats = new Statistics().avg(new BigDecimal("0.00")).min(new BigDecimal("0.00")).max(new BigDecimal("0.00")).count(0L)

    when:
    ticksBucket1.each {
      tick -> sut.updateValues(tick, partialAgg1)
    }
    ticksBucket2.each {
      tick -> sut.updateValues(tick, partialAgg2)
    }
    sut.removePartialAggregation(partialAgg1)
    sut.removePartialAggregation(partialAgg2)

    then:
    sut.getStatistics() == emptyStats
  }

  def "removePartialAggregation ignores empty PartialAggregations"() {
    given:
    def epochSecond = 1590839940L
    def sut = new TotalAggregation()
    def ticks = [new Tick().instrument("ABC").price(new BigDecimal("25.22")).timestamp(epochSecond),
                 new Tick().instrument("DEF").price(new BigDecimal("150.43")).timestamp(epochSecond)]
    def partialAgg1 = createPartialTickAggregation(ticks)
    def emptyPartialAggregation = new PartialAggregation()

    and:
    ticks.each {
      tick -> sut.updateValues(tick, partialAgg1)
    }

    when:
    sut.removePartialAggregation(emptyPartialAggregation)

    then:
    def totalStats = sut.getStatistics()
    totalStats.getMin() == ticks.stream().map(t -> t.getPrice()).min(Comparator.naturalOrder()).get()
    totalStats.getMax() == ticks.stream().map(t -> t.getPrice()).max(Comparator.naturalOrder()).get()
    totalStats.getAvg() == new BigDecimal("87.83")
    totalStats.getCount() == ticks.size()
  }

  private PartialAggregation createPartialTickAggregation(List<Tick> ticks) {
    def partialAggregation = new PartialAggregation()
    ticks.each {
      tick -> partialAggregation.updateValues(tick)
    }
    return partialAggregation
  }
}

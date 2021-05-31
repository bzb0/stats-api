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

import spock.lang.Specification

class AggregatedValuesTest extends Specification {

  def "AggregatedValues constructor creates AggregatedValues object with default values"() {
    when:
    def result = new AggregatedValues()

    then:
    result.getMinPrice() == BigDecimal.ZERO
    result.getMaxPrice() == BigDecimal.ZERO
    result.getPriceSum() == BigDecimal.ZERO
    result.getCount() == 0
    result.isBlank()
  }

  def "updateValues updates the aggregated values with the new tick price"() {
    given:
    def tickPrice = new BigDecimal("25.55")
    def sut = new AggregatedValues()

    when:
    sut.updateValues(tickPrice)

    then:
    sut.getMinPrice() == tickPrice
    sut.getMaxPrice() == tickPrice
    sut.getPriceSum() == tickPrice
    sut.getCount() == 1
  }

  def "updateValues sets min and max tick price according to the last min/max tick price"() {
    given:
    def minTickPrice = new BigDecimal("51.11")
    def maxTickPrice = new BigDecimal("88.42")
    def sut = new AggregatedValues()

    when:
    sut.updateValues(minTickPrice)
    sut.updateValues(maxTickPrice)

    then:
    sut.getMinPrice() == minTickPrice
    sut.getMaxPrice() == maxTickPrice
    sut.getPriceSum() == minTickPrice + maxTickPrice
    sut.getCount() == 2
  }

  def "subtractPartialAggregation subtracts the number of ticks and the sum tick price, and sets the new min and max tick price"() {
    given:
    def sut = new AggregatedValues()
    def tickPrices = [new BigDecimal("55.21"), new BigDecimal("88.41"),
                      new BigDecimal("12.23"), new BigDecimal("40.54"), new BigDecimal("35.00")] as List<Double>

    tickPrices.each {
      sut.updateValues(it)
    }

    def tickPrice = new BigDecimal("12.23")
    def newMinPrice = new BigDecimal("35.00")
    def newMaxPrice = new BigDecimal("88.41")
    def subtractAgg = new AggregatedValues()
    subtractAgg.updateValues(tickPrice)

    when:
    sut.subtractPartialAggregation(subtractAgg, newMinPrice, newMaxPrice)

    then:
    sut.getPriceSum() == tickPrices.sum() - subtractAgg.getPriceSum()
    sut.getCount() == tickPrices.size() - 1
    sut.getMinPrice() == newMinPrice
    sut.getMaxPrice() == newMaxPrice
  }

  def "getAvgPrice returns the average price for the aggregated values"() {
    given:
    def sut = new AggregatedValues()
    def tickPrices = [new BigDecimal("55.21"), new BigDecimal("88.41"),
                      new BigDecimal("12.23"), new BigDecimal("40.54"), new BigDecimal("35.00")] as List<Double>

    and:
    tickPrices.each {
      sut.updateValues(it)
    }

    when:
    def result = sut.getAvgPrice()

    then:
    result == new BigDecimal("46.28")
  }

  def "getAvgPrice returns BigDecimal.ZERO for blank aggregated values"() {
    given:
    def sut = new AggregatedValues()

    when:
    def result = sut.getAvgPrice()

    then:
    result == BigDecimal.ZERO
  }
}

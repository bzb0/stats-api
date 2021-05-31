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
package com.solactive.stats.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Value class that stores aggregated values (min. price, max. price, price sum, tick count). The class doesn't explicitly stores the average price,
 * as it can be calculated when an {@link AggregatedValues} instance will be used to generate the statistics.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class AggregatedValues {

  private BigDecimal minPrice = null;
  private BigDecimal maxPrice = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
  private BigDecimal priceSum = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
  private long count = 0L;

  /**
   * Updates the aggregated values (min, max, sum, count) with the next tick/instrument price.
   *
   * @param tickPrice The new instrument price.
   */
  void updateValues(BigDecimal tickPrice) {
    count++;
    priceSum = priceSum.add(tickPrice);
    minPrice = minPrice == null ? tickPrice : minPrice.min(tickPrice);
    maxPrice = maxPrice.max(tickPrice);
  }

  /**
   * Subtracts a partial aggregation (for single second) from the current total aggregation (for the last 60 seconds). For brevity this method was
   * defined in this class and not in a subclass (e.g. TotalAggregatedValues).
   *
   * @param partialAggregatedValues Partial aggregation that will be subtracted from the current aggregation.
   * @param newMinPrice The new min price for the total aggregation.
   * @param newMaxPrice The new max price for the total aggregation.
   */
  void subtractPartialAggregation(AggregatedValues partialAggregatedValues, BigDecimal newMinPrice, BigDecimal newMaxPrice) {
    count -= partialAggregatedValues.getCount();
    priceSum = priceSum.subtract(partialAggregatedValues.getPriceSum());
    minPrice = newMinPrice;
    maxPrice = newMaxPrice;
  }

  /**
   * Returns {@code true} if aggregation is empty (there were no ticks for given period).
   *
   * @return {@code true} if this aggregation doesn't contain any data, {@link false} otherwise.
   */
  boolean isBlank() {
    return count == 0;
  }

  /**
   * Returns the minimal price.
   *
   * @return The minimal price.
   */
  BigDecimal getMinPrice() {
    return minPrice == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : minPrice;
  }

  /**
   * Returns the average price for the aggregated sum and number of ticks.
   *
   * @return The average price.
   */
  BigDecimal getAvgPrice() {
    if(count == 0) {
      return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
    BigDecimal countBigDecimal = new BigDecimal(count).setScale(2, RoundingMode.HALF_UP);
    return priceSum.divide(countBigDecimal, 2, RoundingMode.HALF_UP);
  }
}

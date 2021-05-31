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

import com.solactive.stats.StatisticsApp
import com.solactive.stats.generated.openapi.model.Tick
import com.solactive.stats.model.AggregatedValues
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import spock.lang.Specification

import javax.validation.ClockProvider
import java.math.RoundingMode
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.ThreadLocalRandom
import java.util.stream.Collectors
import java.util.stream.IntStream

@SpringBootTest(classes = StatisticsApp)
@AutoConfigureMockMvc
@EnableAutoConfiguration
@ActiveProfiles(profiles = ["test2"])
class MultiThreadedStatisticsControllerTest extends Specification {

  private static INSTRUMENTS = ["ABC", "DEF", "GHI", "JKL", "MNO", "PQR", "STU", "VWX", "YZ0", "123"]

  @Autowired
  private MockMvc mvc

  @Autowired
  ClockProvider clockProvider;

  def "GET /statistics for stored 10000 ticks from different threads returns HTTP 200 with correct results"() {
    given:
    def totalAgg = new AggregatedValues()
    def instAgg = new AggregatedValues()
    def executorService = Executors.newFixedThreadPool(10)

    def tickCallables = []
    for (int j = 0; j < 10; j++) {
      def ticks = createRandomTicks(1000, 10)
      ticks.each {
        tick ->
          totalAgg.updateValues(tick.getPrice())
          if (tick.getInstrument() == INSTRUMENTS[0]) {
            instAgg.updateValues(tick.getPrice())
          }
      }

      tickCallables += ticks.stream().map(tick -> new Callable<Void>() {
        @Override
        Void call() throws Exception {
          mvc.perform(MockMvcRequestBuilders.post("/ticks")
              .contentType(MediaType.APPLICATION_JSON)
              .content(convertTickToJSON(tick)))
        }
      }).collect(Collectors.toList())
    }

    executorService.invokeAll(tickCallables)

    when:
    def totalStats = mvc.perform(MockMvcRequestBuilders.get("/statistics"))

    then:
    totalStats.andExpect(MockMvcResultMatchers.content().json("{\"avg\":" + totalAgg.getAvgPrice() + ",\"max\":" + totalAgg.getMaxPrice() +
        ",\"min\":" + totalAgg.getMinPrice() + ",\"count\":" + totalAgg.getCount() + "}"))

    when:
    def instrumentStats = mvc.perform(MockMvcRequestBuilders.get("/statistics/" + INSTRUMENTS[0]))

    then:
    instrumentStats.andExpect(MockMvcResultMatchers.content().json("{\"avg\":" + instAgg.getAvgPrice() + ",\"max\":" + instAgg.getMaxPrice() +
        ",\"min\":" + instAgg.getMinPrice() + ",\"count\":" + instAgg.getCount() + "}"))
  }

  private List<Tick> createRandomTicks(int numTicks, int delaySeconds) {
    def rnd = ThreadLocalRandom.current()
    def currentInstant = clockProvider.getClock().instant()
    return IntStream.range(1, numTicks + 1)
        .mapToObj(i -> new Tick()
            .price(new BigDecimal(rnd.nextDouble(250.0)).setScale(2, RoundingMode.HALF_UP))
            .instrument(INSTRUMENTS[rnd.nextInt(INSTRUMENTS.size())])
            .timestamp(currentInstant.minusSeconds(rnd.nextInt(delaySeconds)).toEpochMilli()))
        .collect(Collectors.toList())
  }

  private String convertTickToJSON(Tick tick) {
    return "{\"instrument\":\"" + tick.getInstrument() + "\",\"price\": " + tick.getPrice() + " ,\"timestamp\":" + tick.getTimestamp() + "}"
  }
}

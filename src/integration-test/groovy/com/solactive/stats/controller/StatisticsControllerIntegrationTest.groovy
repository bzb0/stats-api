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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import spock.lang.Specification
import spock.lang.Stepwise

import javax.validation.ClockProvider

@Stepwise
@SpringBootTest(classes = StatisticsApp)
@AutoConfigureMockMvc
@EnableAutoConfiguration
@ActiveProfiles(profiles = ["test1"])
class StatisticsControllerIntegrationTest extends Specification {

  @Autowired
  private MockMvc mvc

  @Autowired
  ClockProvider clockProvider;

  def "GET /statistics returns HTTP 200 with an empty Statistics object"() {
    expect:
    mvc.perform(MockMvcRequestBuilders.get("/statistics"))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(MockMvcResultMatchers.content().json("{\"avg\":0.00,\"max\":0.00,\"min\":0.00,\"count\":0}"))
  }

  def "GET /statistics/{instrumentId} returns HTTP 200 with an empty Statistics object"() {
    expect:
    mvc.perform(MockMvcRequestBuilders.get("/statistics/ABC"))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(MockMvcResultMatchers.content().json("{\"avg\":0.00,\"max\":0.00,\"min\":0.00,\"count\":0}"))
  }

  def "POST /ticks returns HTTP 204 if the tick is older than 60 seconds"() {
    given:
    def timestamp = clockProvider.getClock().instant().minusSeconds(65).toEpochMilli()
    def tickJson = "{\"instrument\":\"ABC\",\"price\":116.82,\"timestamp\":" + timestamp + "}"

    when:
    def result = mvc.perform(MockMvcRequestBuilders.post("/ticks")
        .contentType(MediaType.APPLICATION_JSON)
        .content(tickJson))

    then:
    result.andExpect(MockMvcResultMatchers.status().isNoContent())
  }

  def "POST /ticks returns HTTP 201 without body if the tick was successfully stored"() {
    given:
    def timestamp = clockProvider.getClock().instant().toEpochMilli()
    def tickJson = "{\"instrument\":\"ABC\",\"price\":116.82,\"timestamp\":" + timestamp + "}"

    when:
    def result = mvc.perform(MockMvcRequestBuilders.post("/ticks")
        .contentType(MediaType.APPLICATION_JSON)
        .content(tickJson))

    then:
    result.andExpect(MockMvcResultMatchers.status().isCreated())
        .andExpect(MockMvcResultMatchers.content().string(""))
  }

  def "GET /statistics returns HTTP 200 with a Statistics object for the previously stored tick (ABC=116.82)"() {
    expect:
    mvc.perform(MockMvcRequestBuilders.get("/statistics"))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(MockMvcResultMatchers.content().json("{\"avg\":116.82,\"max\":116.82,\"min\":116.82,\"count\":1}"))
  }

  def "GET /statistics/{instrumentId} returns HTTP 200 with a Statistics object for a given instrument id"() {
    given:
    def instrumentId = "ABC"

    when:
    def result = mvc.perform(MockMvcRequestBuilders.get("/statistics/" + instrumentId))

    then:
    result.andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(MockMvcResultMatchers.content().json("{\"avg\":116.82,\"max\":116.82,\"min\":116.82,\"count\":1}"))
  }
}

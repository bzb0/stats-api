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
package com.solactive.stats.config;

import java.time.Clock;
import javax.validation.ClockProvider;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * A custom clock provider.
 */
@Configuration
public class ClockConfig {

  /**
   * Returns a custom clock provider that returns the {@link Clock#systemUTC()}.
   *
   * @return The {@link Clock#systemUTC()}.
   */
  @Bean
  @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
  public ClockProvider getClock() {
    return () -> Clock.systemUTC();
  }
}

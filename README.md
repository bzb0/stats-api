# Solactive Code Challenge - Stats API

### Description

The Stats API is implemented in Java 11 using the Spring Boot (2.5.0) framework and uses Gradle (7.0.2) as a build and dependency tool. The Stats API
uses the following technologies/dependencies:

* [Spring Web](https://spring.io/web-applications) - REST API
* [JSON Problem Details](https://github.com/zalando/problem-spring-web) - JSON Problem implementation
* [OpenAPI Specification](https://swagger.io/specification/) - Stats API contract
* [OpenAPI Generator plugin](https://github.com/OpenAPITools/openapi-generator/tree/master/modules/openapi-generator-gradle-plugin) - server stub
  generation
* [Lombok](https://projectlombok.org/) - code generation
* [Spock Framework](https://spockframework.org/) - test framework

### Building the Stats API

**In order to be able to execute the integration tests, the dependency *net.bull.javamelody:javamelody-spring-boot-starter:1.81.0* has to be removed,
as a NullPointerException is thrown in the class net.bull.javamelody.MonitoringFilter.**

The following command compiles the Stats API and executes all tests (unit & integraiton):

```
./gradlew clean build
```

The tests (unit & integration) can be executed separately with the following command:

```
./gradlew clean check
```

### Starting the Stats API

The Stats API can be started with the following command:

```
./gradlew bootRun
```

The Stats API will be available at port **8080** under the context root **/stats-api**. The following endpoints will be available:

* http://localhost:8080/stats-api/ticks
* http://localhost:8080/stats-api/statistics
* http://localhost:8080/stats-api/statistics/{instrumentId}

## Discussion

### Assumptions

* The tick timestamp will not be a future timestamp.

### Improvements

* The **InstrumentAggregator** uses the built-in Java synchronization to perform locking operations. This can lead to poor performance under heavy
  loads (thousands requests on POST /ticks). This can be improved by using a non-blocking concurrency utilities.
* The sliding window aggregation is implemented with the *Subtract-on-evict* algorithm. This can be improved with a better algorithm such as
  *De-Amortized Banker's Aggregator (DABA)*.
* Load tests could be written to test the API under heavy load.
* A Java microbenchmark (JMH) for the different components could be written.
* Currently, a single request will be processed on the request thread, which will eventually block many concurrent requests. This can be improved by
  specifying a dedicated thread pool for the tick processing.
* Currently, **BigDecimal** is used for storing the aggregated values, which incurs an additional memory usage. The aggregated values can be
  efficiently stored using a monetary library.
* The **InstrumentAggregator** sliding window implementation could be rewritten as a circular buffer with a fixed size.

### Did I Like It?

Yes, I really liked the coding challenge, due to its real-time and constant access time (O(1)) requirements. Is there a reference solution for the
coding challenge?

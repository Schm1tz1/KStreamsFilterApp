# Example KStreams Filter App
[![Java CI with Maven](https://github.com/Schm1tz1/KStreamsFilterApp/actions/workflows/maven.yml/badge.svg)](https://github.com/Schm1tz1/KStreamsFilterApp/actions/workflows/maven.yml)

## Processing Logic
- configurable filtering app for JSON objects, i.e. forward/drop objects in case a configured field in the payload matches a specific pattern 

## Data Format / (De-)Serialization
- Messages are String-encoded JSON without usage of schema registry
- As there is no further processing logic involved, String SerDes are used and messages are forwarded as such

## Build
- using maven: `mvn compile`
- packaging to jar files: `mvn package`

## Testing
- Unit test for components and TTD tests are included
- included in maven build, separate test run: `mvn test`

## Application Configuration
- The Kafka Streams application can handle a configurable number of filtering pipelines
- The application needs a few properties to be defined (also see the test configurations in folder src/test/resources/*.properties):
  ```properties
  streamsFilter.sources = Source1,Source2

  # Source 1
  streamsFilter.Source1.inputTopic = topic.Source1.unfiltered
  streamsFilter.Source1.outputTopic = topic.Source1
  streamsFilter.Source1.field = path.to.json.field
  streamsFilter.Source1.filterPattern = 42
  streamsFilter.Source1.filterActionDrop = false

  # Source 2
  streamsFilter.Source2.inputTopic = topic.Source2.unfiltered
  streamsFilter.Source2.outputTopic = topic.Source2
  streamsFilter.Source2.field = path.to.json.field
  streamsFilter.Source2.filterPattern = dont_panic
  streamsFilter.Source2.filterActionDrop = true
  ```
- The Kafka Streams application should be configured for *durability* (default is availability/performance) to avoid data loss in case of crashes, also see [Configuring a Streams Application](https://docs.confluent.io/platform/current/streams/developer-guide/config-streams.html#recommended-configuration-parameters-for-resiliency). The following properties should be set:
  ```properties
  acks=all
  replication.factor=3
  num.standby.replicas=1
  ```
## Deployment, Running
- A fat jar file is created in addition to avoid dependency issues that can be use for single-file-deployments
- One Kafka Streams application needs to be configured and started per pipeline
- The application is started with java command, e.g.
  ```bash
  java -jar target/KStreamsFilterApp-0.1-jar-with-dependencies.jar <command-line arguments>
  ```
- Help on arguments:
  ```bash
  java -jar target/KStreamsFilterApp-0.1-jar-with-dependencies.jar -h
  ```
- Example using a configuration file:
  ```bash
  java -jar KStreamsFilterApp-0.1-jar-with-dependencies.jar --config-file streams_other.properties
  ```
- Example run with local Kafka broker and prometheus agent:
  ```bash
  java -javaagent:target/jmx_prometheus_javaagent-0.17.2.jar=1234:configs/jmx_exporter_kafka_streams.yml -jar target/KStreamsFilterApp-0.1-jar-with-dependencies.jar -c examples/streams_localhost.properties
  ```
- Docker-based:
  - Docker examples and run scrips can be found in docker.
  - Nothing special - rather straightforward: run a fat jar in docker and mount configurations/certificates.

## Log Format
- Default configuration is built into the jar but can be overridden on the command line by passing a configuration file using `-Dlog4j.configuration`, example:
  ```bash
  java -jar target/KStreamsFilterApp-0.1-jar-with-dependencies.jar -Dlog4j.configuration=file:/path/to/log4jconfig.properties
  ```
- Logging is per default done on INFO level to STDOUT using slf4j-simple

## Metrics
- Per default no additional metrics are exposed.
- JMX remote monitoring is possible, you need to add the corresponding properties to the java command line, example for non-encrypted JMX without authentication on port 8888:
  ```bash
  java -Dcom.sun.management.jmxremote=true -Dcom.sun.management.jmxremote.port=8888 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -jar target/KStreamsFilterApp-0.1-jar-with-dependencies.jar -c examples/streams_combined_TLS.properties
  ```
- Streams metrics, lag end e2e latency can be seen in the Confluent Control Center once enabling monitoring interceptors using the `--enable-monitoring-interceptor` argument. You will need to set the security for the interceptors explicitly, otherwise it will assume default *bootstrap.servers* without security (also see example configurations).
- Prometheus JMX agent can be added with `-javaagent:<path-to-agent>/jmx_prometheus_javaagent-0.17.2.jar=1234:<path-to-config>/jmx_exporter_kafka_streams.yml`

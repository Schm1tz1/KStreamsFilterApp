package com.github.schm1tz1;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

class EventFilterPipelineTtdTest {

    final static Logger logger = LoggerFactory.getLogger(EventFilterPipelineTtdTest.class);
    final static String[] processorApiSwitch = {"false", "true"};
    private static TestInputTopic<String, String> inputTopicFirst;
    private static TestInputTopic<String, String> inputTopicSecond;
    private static TestOutputTopic<String, String> outputTopicFirst;
    private static TestOutputTopic<String, String> outputTopicSecond;

    private TopologyTestDriver createTopologyTestDriverFromProperties(Properties streamProperties) {
        EventFilterPipeline eventFilterPipeline = new EventFilterPipeline(streamProperties);
        Topology topology = eventFilterPipeline.createMultiTopology();
        TopologyTestDriver topologyTestDriver = new TopologyTestDriver(topology);

        inputTopicFirst = topologyTestDriver.createInputTopic(streamProperties.getProperty("streamsFilter.First.inputTopic"),
                Serdes.String().serializer(), Serdes.String().serializer());

        outputTopicFirst = topologyTestDriver.createOutputTopic(streamProperties.getProperty("streamsFilter.First.outputTopic"),
                Serdes.String().deserializer(), Serdes.String().deserializer());

        inputTopicSecond = topologyTestDriver.createInputTopic(streamProperties.getProperty("streamsFilter.Second.inputTopic"),
                Serdes.String().serializer(), Serdes.String().serializer());

        outputTopicSecond = topologyTestDriver.createOutputTopic(streamProperties.getProperty("streamsFilter.Second.outputTopic"),
                Serdes.String().deserializer(), Serdes.String().deserializer());

        return topologyTestDriver;
    }

    void readAdditionalProperties(Properties properties, String fileName) {
        logger.info("Reading additional test configuration from " + fileName);
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName)) {
            properties.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    List<String> readStringFile(String fileName) {
        logger.info("Reading file " + fileName);
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName)) {
            return Arrays.asList(new String(inputStream.readAllBytes()).split("\n"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Test
    void testCombinedEventPipelineSingleMessage() {

        for (String apiSwitch : processorApiSwitch) {
            logger.info("PAPI usage: "+apiSwitch);
            Properties testProperties = PipelineConfigTools.setDefaultStreamsProperties();
            readAdditionalProperties(testProperties, "streams_combined_test.properties");
            testProperties.put("use.processor.api", apiSwitch);

            TopologyTestDriver topologyTestDriverFromProperties = createTopologyTestDriverFromProperties(testProperties);

            String testMessageFirstNonHumiditySensor = "{\"entity_id\": \"sensor.sm_g950f_battery_temperature\", \"state\": \"28.6\", \"attributes\": {\"state_class\": \"measurement\", \"unit_of_measurement\": \"\\u00b0C\", \"device_class\": \"temperature\", \"icon\": \"mdi:battery\", \"friendly_name\": \"SM-G950F Battery Temperature\"}, \"last_changed\": \"2023-01-17T22:29:09.375264+00:00\", \"last_updated\": \"2023-01-17T22:29:09.375264+00:00\", \"context\": {\"id\": \"01GQ0XKE3ZYMZH2P2JVN66NGE2\", \"parent_id\": null, \"user_id\": null}}";
            String testMessageFirstHumiditySensor = "{\"entity_id\": \"sensor.temperature_humidity_sensor_b99f_battery\", \"state\": \"65\", \"attributes\": {\"state_class\": \"measurement\", \"unit_of_measurement\": \"%\", \"device_class\": \"battery\", \"friendly_name\": \"Temperature/Humidity Sensor B99F Battery\"}, \"last_changed\": \"2023-01-17T22:11:12.880959+00:00\", \"last_updated\": \"2023-01-17T22:11:12.880959+00:00\", \"context\": {\"id\": \"01GQ0WJJVG1BMFA4AW4KBEBWG0\", \"parent_id\": null, \"user_id\": null}}";
            String testMessageSecondNonDewPoint = "{\"entity_id\": \"sensor.basement_thermal_comfort_humidex\", \"state\": \"14.7\", \"attributes\": {\"state_class\": \"measurement\", \"temperature\": 15.7, \"humidity\": 44.6, \"unit_of_measurement\": \"\\u00b0C\", \"device_class\": \"temperature\", \"icon\": \"mdi:sun-thermometer\", \"friendly_name\": \"Basement Thermal Comfort Humidex\"}, \"last_changed\": \"2023-01-17T22:01:23.241070+00:00\", \"last_updated\": \"2023-01-17T22:01:43.247303+00:00\", \"context\": {\"id\": \"01GQ0W16JFGMXYKE83QHK9BBV2\", \"parent_id\": null, \"user_id\": null}}";
            String testMessageSecondDewPoint = "{\"entity_id\": \"sensor.basement_thermal_comfort_dewpoint\", \"state\": \"3.72\", \"attributes\": {\"state_class\": \"measurement\", \"temperature\": 15.7, \"humidity\": 44.7, \"unit_of_measurement\": \"\\u00b0C\", \"device_class\": \"temperature\", \"icon\": \"mdi:thermometer-water\", \"friendly_name\": \"Basement Thermal Comfort Dew point\"}, \"last_changed\": \"2023-01-17T22:02:58.175114+00:00\", \"last_updated\": \"2023-01-17T22:02:58.175114+00:00\", \"context\": {\"id\": \"01GQ0W3FQZRVNN51HZYMF4DC75\", \"parent_id\": null, \"user_id\": null}}";

            inputTopicFirst.pipeInput(testMessageFirstNonHumiditySensor);
            inputTopicFirst.pipeInput(testMessageFirstHumiditySensor);
            inputTopicSecond.pipeInput(testMessageSecondNonDewPoint);
            inputTopicSecond.pipeInput(testMessageSecondDewPoint);
            
            Assertions.assertEquals(1, outputTopicFirst.getQueueSize());
            Assertions.assertEquals(testMessageFirstHumiditySensor, outputTopicFirst.readValue());
            Assertions.assertEquals(1, outputTopicSecond.getQueueSize());
            Assertions.assertEquals(testMessageSecondNonDewPoint, outputTopicSecond.readValue());
            
            topologyTestDriverFromProperties.close();
        }
    }
    @Test
    void testCombinedEventPipelineFromFiles() {

        for (String apiSwitch : processorApiSwitch) {
            logger.info("PAPI usage: "+apiSwitch);
            Properties testProperties = PipelineConfigTools.setDefaultStreamsProperties();
            readAdditionalProperties(testProperties, "streams_combined_test.properties");
            testProperties.put("use.processor.api", apiSwitch);

            TopologyTestDriver topologyTestDriverFromProperties = createTopologyTestDriverFromProperties(testProperties);

            List<String> thermalComfortList = readStringFile("thermal_comfort_1000.json");
            List<String> batteryList = readStringFile("battery_1000.json");

            logger.info("Test messages (topic dump): " + batteryList.size() + " battery events, " + thermalComfortList.size() + " thermal comfort events");

            inputTopicFirst.pipeValueList(batteryList);
            inputTopicSecond.pipeValueList(thermalComfortList);

            logger.info("Filtered messages: " + outputTopicFirst.getQueueSize() + " battery events, " + outputTopicSecond.getQueueSize() + " thermal comfort events");

            Assertions.assertEquals(553, outputTopicFirst.getQueueSize() ); // 553 matching events in test data -> should be forwarded
            Assertions.assertEquals(thermalComfortList.size()-125, outputTopicSecond.getQueueSize() ); // 125 matching events in test data -> should be dropped

            topologyTestDriverFromProperties.close();
        }
    }
}
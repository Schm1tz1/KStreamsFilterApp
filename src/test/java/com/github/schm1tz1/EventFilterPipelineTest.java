package com.github.schm1tz1;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;

class EventFilterPipelineTest {

    final static Logger logger = LoggerFactory.getLogger(EventFilterPipelineTest.class);
    String testMessageFirstNonB99F = "{\"entity_id\": \"sensor.sm_g950f_battery_temperature\", \"state\": \"28.6\", \"attributes\": {\"state_class\": \"measurement\", \"unit_of_measurement\": \"\\u00b0C\", \"device_class\": \"temperature\", \"icon\": \"mdi:battery\", \"friendly_name\": \"SM-G950F Battery Temperature\"}, \"last_changed\": \"2023-01-17T22:29:09.375264+00:00\", \"last_updated\": \"2023-01-17T22:29:09.375264+00:00\", \"context\": {\"id\": \"01GQ0XKE3ZYMZH2P2JVN66NGE2\", \"parent_id\": null, \"user_id\": null}}";
    String testMessageFirstB99F = "{\"entity_id\": \"sensor.temperature_humidity_sensor_b99f_battery\", \"state\": \"65\", \"attributes\": {\"state_class\": \"measurement\", \"unit_of_measurement\": \"%\", \"device_class\": \"battery\", \"friendly_name\": \"Temperature/Humidity Sensor B99F Battery\"}, \"last_changed\": \"2023-01-17T22:11:12.880959+00:00\", \"last_updated\": \"2023-01-17T22:11:12.880959+00:00\", \"context\": {\"id\": \"01GQ0WJJVG1BMFA4AW4KBEBWG0\", \"parent_id\": null, \"user_id\": null}}";
    String testMessageSecondNonDewPoint = "{\"entity_id\": \"sensor.basement_thermal_comfort_humidex\", \"state\": \"14.7\", \"attributes\": {\"state_class\": \"measurement\", \"temperature\": 15.7, \"humidity\": 44.6, \"unit_of_measurement\": \"\\u00b0C\", \"device_class\": \"temperature\", \"icon\": \"mdi:sun-thermometer\", \"friendly_name\": \"Basement Thermal Comfort Humidex\"}, \"last_changed\": \"2023-01-17T22:01:23.241070+00:00\", \"last_updated\": \"2023-01-17T22:01:43.247303+00:00\", \"context\": {\"id\": \"01GQ0W16JFGMXYKE83QHK9BBV2\", \"parent_id\": null, \"user_id\": null}}";
    String testMessageSecondDewPoint = "{\"entity_id\": \"sensor.basement_thermal_comfort_dewpoint\", \"state\": \"3.72\", \"attributes\": {\"state_class\": \"measurement\", \"temperature\": 15.7, \"humidity\": 44.7, \"unit_of_measurement\": \"\\u00b0C\", \"device_class\": \"temperature\", \"icon\": \"mdi:thermometer-water\", \"friendly_name\": \"Basement Thermal Comfort Dew point\"}, \"last_changed\": \"2023-01-17T22:02:58.175114+00:00\", \"last_updated\": \"2023-01-17T22:02:58.175114+00:00\", \"context\": {\"id\": \"01GQ0W3FQZRVNN51HZYMF4DC75\", \"parent_id\": null, \"user_id\": null}}";

    @Test
    void testCreateTopologyWithValidProperties() {
        Properties testProperties = PipelineConfigTools.setDefaultStreamsProperties();
        testProperties.put("streamsFilter.sources", "Test");
        testProperties.put("streamsFilter.Test.inputTopic", "TestIn");
        testProperties.put("streamsFilter.Test.outputTopic", "TestOut");
        testProperties.put("streamsFilter.Test.field", "TestKey");
        testProperties.put("streamsFilter.Test.filterPattern", "42");
        testProperties.put("streamsFilter.Test.filterActionDrop", "false");
        assertNotNull(new EventFilterPipeline(testProperties).createMultiTopology());
    }

    @Test
    void testThrowExceptionOnInvalidSettings() {
        Properties testProperties = new Properties();
        logger.info("Checking (invalid) properties: " + testProperties);

        assertThrows(RuntimeException.class, () -> {
            new EventFilterPipeline(testProperties).createMultiTopology();
        });
    }

    @Test
    void testGetStringForPath() {
        String testMessage = "{\"id\":\"0001\",\"type\":\"donut\",\"name\":\"Cake\",\"image\":{\"url\":\"images/0001.jpg\",\"width\":200,\"height\":200},\"thumbnail\":{\"url\":\"images/thumbnails/0001.jpg\",\"width\":32,\"height\":32}}";
        logger.info("Testing JSON path retrieval method.");
        Assertions.assertEquals("200", EventFilterPipelineTools.getStringForPath(testMessage, "image.width"));
        Assertions.assertEquals("32", EventFilterPipelineTools.getStringForPath(testMessage, "thumbnail.height"));
        Assertions.assertEquals("images/thumbnails/0001.jpg", EventFilterPipelineTools.getStringForPath(testMessage, "thumbnail.url"));
    }

    @Test
    void testJsonParserCrashesNullInput() {
        logger.info("Testing for handling of null input JSON messages.");
        assertThrows(NullPointerException.class, () -> {
            EventFilterPipelineTools.getStringForPath(null, "not.here");
        });
    }

    @Test
    void testJsonParserSkipsOnInvalidInput() {
        logger.info("Testing for handling of invalid JSON messages - should log an error.Ø");
        String jsonString = "{ test: data, invalid-json }";
        String returnValue = EventFilterPipelineTools.getStringForPath(jsonString, "not.here");
        assertEquals("", returnValue);
    }

    @Test
    void testPredicateB99FEventStringMatching() {
        logger.info("Testing predicate for events to be dropped.");
        assertFalse(EventFilterPipelineTools.isEventMatchingThePattern(testMessageFirstNonB99F, "attributes.friendly_name", "Temperature/Humidity Sensor B99F Battery"));
    }

    @Test
    void testPredicateNonB99FEventStringMatching() {
        logger.info("Testing predicate for events to be forwarded.");
        assertTrue(EventFilterPipelineTools.isEventMatchingThePattern(testMessageFirstB99F, "attributes.friendly_name", "Temperature/Humidity Sensor B99F Battery"));
    }

    @Test
    void testPredicateB99FEventRegexMatching() {
        logger.info("Testing predicate for events to be dropped.");
        assertFalse(EventFilterPipelineTools.isEventMatchingThePattern(testMessageFirstNonB99F, "attributes.friendly_name", ".*B99F.*"));
    }

    @Test
    void testPredicateNonB99FEventRegexMatching() {
        logger.info("Testing predicate for events to be forwarded.");
        assertTrue(EventFilterPipelineTools.isEventMatchingThePattern(testMessageFirstB99F, "attributes.friendly_name", ".*B99F.*"));
    }

    @Test
    void testPredicateDewPointEventStringMatching() {
        logger.info("Testing predicate for events to be dropped.");
        assertTrue(EventFilterPipelineTools.isEventMatchingThePattern(testMessageSecondDewPoint, "entity_id", "sensor.basement_thermal_comfort_dewpoint"));
    }

    @Test
    void testPredicatePurchaseNonDewPointEventRegexMatching() {
        logger.info("Testing predicate for events to be forwarded.");
        assertFalse(EventFilterPipelineTools.isEventMatchingThePattern(testMessageSecondNonDewPoint, "entity_id", "sensor.(.*)_thermal_comfort_dewpoint"));
    }

    @Test
    void testPredicateDewPointEventRegexMatching() {
        logger.info("Testing predicate for events to be dropped.");
        assertTrue(EventFilterPipelineTools.isEventMatchingThePattern(testMessageSecondDewPoint, "entity_id", "sensor.(.*)_thermal_comfort_dewpoint"));
    }

    @Test
    void testPredicatePurchaseNonDewPointEventStringMatching() {
        logger.info("Testing predicate for events to be forwarded.");
        assertFalse(EventFilterPipelineTools.isEventMatchingThePattern(testMessageSecondNonDewPoint, "entity_id", "sensor.basement_thermal_comfort_dewpoint"));
    }
}
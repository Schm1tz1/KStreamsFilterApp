package com.github.schm1tz1;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Tools for Kafka Streams Pipeline data processing
 */
public class EventFilterPipelineTools {

    final static Logger logger = LoggerFactory.getLogger(EventFilterPipelineTools.class);

    /**
     * Checks if we have a matching event using the logic provided as an argument
     * @param inputMessageValue Input message to be tested (value part)
     * @param jsonPath JSON path to be checked
     * @param patternToMatch Pattern that our value needs to match
     * @return predicate returns true if we match the condition to discard an event
     */
    public static boolean isEventMatchingThePattern(String inputMessageValue, String jsonPath, String patternToMatch) {
        return Pattern.matches(patternToMatch, getStringForPath(inputMessageValue, jsonPath));
    }

    /**
     * Parses and extracts a String object from a given String-based JSON input
     * @param inputJsonString String-encoded JSON input
     * @param path JSON path to field that is to be extracted
     * @return String-encoded value
     */
    public static String getStringForPath(String inputJsonString, String path) {
        logger.trace("predicate input: " + inputJsonString);
        logger.trace("JSON path: " + path);

        try {
            JSONObject jsonObject = new JSONObject(inputJsonString);
            for (String step : path.split("\\.")) {
                if (jsonObject.has(step)) {
                    Object subObject = jsonObject.get(step);
                    logger.trace("next step: " + jsonObject);

                    if (subObject.getClass().equals(JSONObject.class)) {
                        jsonObject = (JSONObject) subObject;
                    } else if (subObject.getClass().equals(String.class)) {
                        logger.trace("Found String Object "+subObject);
                        return (String) subObject;
                    } else if (subObject.getClass().equals(Integer.class)) {
                        logger.trace("Found Integer Object "+subObject);
                        return String.valueOf(subObject);
                    } else {
                        logger.error("Unsupported field format: " + subObject.getClass().getSimpleName());
                    }
                }
            }
        } catch (JSONException err) {
            logger.error("Error in JSON processing, cannot retrieve "+path+" from input String: " + err);
        }
        return "";
    }
}

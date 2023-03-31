package com.github.schm1tz1;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;

/**
 * Kafka Streams Pipeline for filtering
 */
public class EventFilterPipeline {
    final static Logger logger = LoggerFactory.getLogger(EventFilterPipeline.class);
    final Properties streamsProperties;

    /**
     * Constructor to create App with properties
     * @param streamsProperties Properties for the Kafka Streams application
     */
    public EventFilterPipeline(Properties streamsProperties) {
        logger.debug("Starting EventFilterPipeline additional properties");
        this.streamsProperties = streamsProperties;
    }

    /**
     * Creates topology with multiple (sub-)topologies for the pipelines defined via streamsFilter.sources
     * @return topology object to be used with Kafka Streams
     */
    public Topology createMultiTopology() {

        final StreamsBuilder builder = new StreamsBuilder();

        String usePapi = streamsProperties.getProperty("use.processor.api", "false");
        String[] sources = PipelineConfigTools.getPropertyChecked(streamsProperties, "streamsFilter.sources").split(",");

        for (String source : sources) {
            if(usePapi.equals("true")) {
                logger.info("Using Processor API !");
                addSubTopologyForSourceProcessorApi(builder, source);
            } else {
                logger.info("Using Streams DSL !");
                addSubTopologyForSource(builder, source);
            }
        }

        final Topology topology = builder.build();
        logger.debug(topology.describe().toString());

        return topology;
    }

    /**
     * Adds a (sub-)topology for filtering based on the sources and predicate defined in properties
     * @param builder Streams Builder needed to generate the full topology
     * @param sourceName Source Name from configuration
     */

    private void addSubTopologyForSource(StreamsBuilder builder, String sourceName) {

        String inputTopicName = PipelineConfigTools.getPropertyChecked(streamsProperties, "streamsFilter." + sourceName + ".inputTopic");
        String outputTopicName = PipelineConfigTools.getPropertyChecked(streamsProperties, "streamsFilter." + sourceName + ".outputTopic");
        String jsonFilterField = PipelineConfigTools.getPropertyChecked(streamsProperties, "streamsFilter." + sourceName + ".field");
        String jsonFilterValue = PipelineConfigTools.getPropertyChecked(streamsProperties, "streamsFilter." + sourceName + ".filterPattern");
        Boolean jsonFilterActionDrop = Boolean.parseBoolean(PipelineConfigTools.getPropertyChecked(streamsProperties, "streamsFilter." + sourceName + ".filterActionDrop"));

        logger.info("Creating sub-topology for " + inputTopicName + " -> " + outputTopicName);
        logger.info(" - Filtering pattern: " + jsonFilterField + " = " + jsonFilterValue);
        logger.info(" - drop matching messages: " + jsonFilterActionDrop + " = " + jsonFilterActionDrop);

        if (jsonFilterActionDrop) {
            builder
                    .stream(inputTopicName,
                            Consumed.with(Serdes.String(), Serdes.String()
                            )
                    )
                    .filterNot((key, value)
                            -> EventFilterPipelineTools.isEventMatchingThePattern(value, jsonFilterField, jsonFilterValue)
                    )

                    .to(outputTopicName);
        } else {
            builder
                    .stream(inputTopicName,
                            Consumed.with(Serdes.String(), Serdes.String()
                            )
                    )
                    .filter((key, value)
                            -> EventFilterPipelineTools.isEventMatchingThePattern(value, jsonFilterField, jsonFilterValue)
                    )

                    .to(outputTopicName);
        }
    }

    private void addSubTopologyForSourceProcessorApi(StreamsBuilder builder, String sourceName) {

        String inputTopicName = PipelineConfigTools.getPropertyChecked(streamsProperties, "streamsFilter." + sourceName + ".inputTopic");
        String outputTopicName = PipelineConfigTools.getPropertyChecked(streamsProperties, "streamsFilter." + sourceName + ".outputTopic");
        String jsonFilterField = PipelineConfigTools.getPropertyChecked(streamsProperties, "streamsFilter." + sourceName + ".field");
        String jsonFilterValue = PipelineConfigTools.getPropertyChecked(streamsProperties, "streamsFilter." + sourceName + ".filterPattern");
        Boolean jsonFilterActionDrop = Boolean.parseBoolean(PipelineConfigTools.getPropertyChecked(streamsProperties, "streamsFilter." + sourceName + ".filterActionDrop"));

        logger.info("Creating sub-topology for " + inputTopicName + " -> " + outputTopicName);
        logger.info(" - Filtering pattern: " + jsonFilterField + " = " + jsonFilterValue);
        logger.info(" - drop matching messages: " + jsonFilterActionDrop + " = " + jsonFilterActionDrop);

        builder
                .stream(inputTopicName,
                        Consumed.with(Serdes.String(), Serdes.String()
                        )
                )
                .process(() -> new StreamFilterProcessor(jsonFilterField, jsonFilterValue, jsonFilterActionDrop))
                .to(outputTopicName, Produced.with(Serdes.String(), Serdes.String()));
    }

    void run() {
        final Topology topology = createMultiTopology();

        final KafkaStreams streams = new KafkaStreams(topology, streamsProperties);
        final CountDownLatch latch = new CountDownLatch(1);

        // attach shutdown handler to catch control-c
        Runtime.getRuntime().addShutdownHook(new Thread("streams-shutdown-hook") {
            @Override
            public void run() {
                streams.close();
                latch.countDown();
            }
        });

        try {
//            streams.cleanUp();
            streams.start();
            latch.await();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        System.exit(0);
    }




}

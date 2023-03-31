package com.github.schm1tz1;
import org.apache.kafka.common.metrics.Sensor;
import org.apache.kafka.streams.StreamsMetrics;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;

public class StreamFilterProcessor implements Processor<String, String, String, String> {
    private Boolean jsonFilterActionDrop;
    private ProcessorContext<String, String> context;
    private String jsonFilterField;
    private String jsonFilterValue;
    private Sensor sensorFiltered;
    private Sensor sensorIn;
    private Sensor sensorOut;

    /**
     * constructor
     *
     * @param jsonFilterField      JSON path to field that is to be checked
     * @param jsonFilterValue      value of JSON field to be checked
     * @param jsonFilterActionDrop
     */
    public StreamFilterProcessor(String jsonFilterField, String jsonFilterValue, Boolean jsonFilterActionDrop)
    {
        this.jsonFilterField = jsonFilterField;
        this.jsonFilterValue = jsonFilterValue;
        this.jsonFilterActionDrop = jsonFilterActionDrop;
    }

    /**
     * @param context the context; may not be null
     */
    @Override
    public void init(ProcessorContext<String, String> context) {
        this.context = context;
        registerCustomMetrics();

        Processor.super.init(context);
    }

    private void registerCustomMetrics() {
        StreamsMetrics streamMetrics = context.metrics();

        sensorIn = streamMetrics.addRateTotalSensor("kstreams-filter",
                context.applicationId(),
                "filter-in",
                Sensor.RecordingLevel.INFO,
                "task-id", "none");

        sensorOut = streamMetrics.addRateTotalSensor("kstreams-filter",
                context.applicationId(),
                "filter-out",
                Sensor.RecordingLevel.INFO,
                "task-id", "none");

        sensorFiltered = streamMetrics.addRateTotalSensor("kstreams-filter",
                context.applicationId(),
                "filter-filtered",
                Sensor.RecordingLevel.INFO,
                "task-id", "none");
    }

    /**
     * @param record the record to process, will be forwarded/dropped if it matches the pattern depending on the configuration
     */
    @Override
    public void process(Record<String, String> record) {
        String value = record.value();
        sensorIn.record();

        // basically we have a XNOR condition to match (i.e. drop if drop-on-match and match both are true or false)
        if(jsonFilterActionDrop == EventFilterPipelineTools.isEventMatchingThePattern(value, this.jsonFilterField, this.jsonFilterValue)) {
            sensorFiltered.record();
        } else {
            sensorOut.record();
            context.forward(record);
        };
    }

    /**
     *
     */
    @Override
    public void close() {
        Processor.super.close();
    }
}

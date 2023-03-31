package com.github.schm1tz1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.Properties;

@CommandLine.Command(name = "KStreamsFilterApp",
        version = "KStreamsFilterApp 0.1",
        description = "Kafka Streams based filter app for Matrixx events.",
        mixinStandardHelpOptions = true)

/**
 * Runnable Filtering App that adds command line parsing and handling
 */
public class KStreamsFilterApp implements Runnable {
    final static Logger logger = LoggerFactory.getLogger(KStreamsFilterApp.class);
    @CommandLine.Option(names = {"-c", "--config-file"},
            description = "If provided, content will be added to the properties")
    protected static String configFile = null;

    @CommandLine.Option(names = {"-C", "--additional-config-file"},
            description = "If provided, will add additional configurations e.g. for overrides and business logic"
    )
    protected static String additionalConfigFile = null;

    @CommandLine.Option(names = {"--enable-monitoring-interceptor"},
            description = "Enable MonitoringInterceptors (for Control Center)")
    protected boolean monitoringInterceptors = false;

    public static void main(String[] args) throws Exception {
        int returnCode = new CommandLine(new KStreamsFilterApp()).execute(args);
        System.exit(returnCode);
    }

    @Override
    public void run() {
        Properties streamProperties = PipelineConfigTools.configureStreamsProperties(configFile, additionalConfigFile);
        if(monitoringInterceptors) {
            PipelineConfigTools.addMonitoringInterceptorConfig(streamProperties);
        }
        EventFilterPipeline eventFilterPipeline = new EventFilterPipeline(streamProperties);
        eventFilterPipeline.run();
    }
}

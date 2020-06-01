package com.oltranz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.slf4j.Logger;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

@SpringBootApplication
public class ProtocolTranslatorApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProtocolTranslatorApplication.class, args);
		// consumeData();
	}

	// private static void consumeData() {
    //     final Logger logger = LoggerFactory.getLogger(ProtocolTranslatorApplication.class);
    //    logger.info("STARTING TO CONSUME DATA: ");

    //    MyKafkaConsumer kafka = new MyKafkaConsumer();
    //    kafka.run();
    // }

}

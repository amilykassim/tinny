package com.oltranz.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.oltranz.model.ResponseModel;

import org.apache.kafka.clients.consumer.ConsumerRecord;

public class Marshaller {
    public static Object xmlToObject(String xml, Class className) {
        try {
            JacksonXmlModule module = new JacksonXmlModule();
            module.setDefaultUseWrapper(false);
            XmlMapper xmlMapper = new XmlMapper(module);
            return xmlMapper.readValue(xml, className);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String objectToXML(Object object) {
        try {
            JacksonXmlModule module = new JacksonXmlModule();
            module.setDefaultUseWrapper(false);
            XmlMapper xmlMapper = new XmlMapper(module);
            xmlMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            xmlMapper.enable(SerializationFeature.INDENT_OUTPUT);
            return xmlMapper.writeValueAsString(object);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

	// public static Object objectToXML(String xml, Class<ResponseModel> className) {
	// 	try {
    //         JacksonXmlModule module = new JacksonXmlModule();
    //         module.setDefaultUseWrapper(false);
    //         XmlMapper xmlMapper = new XmlMapper(module);
    //         return xmlMapper.readValue(xml, className);
    //     } catch (Exception e) {
    //         e.printStackTrace();
    //         return null;
    //     }
	// }

	// public static Object xmlToObject(ConsumerRecord<String, ResponseModel> response, String name) {
    //     JacksonXmlModule module = new JacksonXmlModule();
    //         module.setDefaultUseWrapper(false);
    //         XmlMapper xmlMapper = new XmlMapper(module);
    //         return xmlMapper.readValues(response, name);
	// }
}
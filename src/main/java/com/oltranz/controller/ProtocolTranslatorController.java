package com.oltranz.controller;

import java.util.concurrent.ExecutionException;

import com.oltranz.model.ResponseModel;
import com.oltranz.util.Marshaller;
import com.oltranz.model.GateWayModel;
import com.oltranz.model.RequestModel;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.requestreply.RequestReplyFuture;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = {"/mtn", "/airtel"})
public class ProtocolTranslatorController {

	@Value("${kafka.request.topic}")
	private String requestTopic;

	@Autowired
	private ReplyingKafkaTemplate<String, GateWayModel, ResponseModel> replyingKafkaTemplate;

	@PostMapping("/request")
	public ResponseEntity<Object> getObject(@RequestBody RequestModel incomingRequest)
			throws InterruptedException, ExecutionException {

		GateWayModel request = incomingRequest.convertToGateWayModel();
		ProducerRecord<String, GateWayModel> record = new ProducerRecord<>(requestTopic, null, request);
		RequestReplyFuture<String, GateWayModel, ResponseModel> future = replyingKafkaTemplate.sendAndReceive(record);
		ConsumerRecord<String, ResponseModel> response = future.get();

		Object result = Marshaller.objectToXML(response.value().toString());
		return new ResponseEntity<>(result, HttpStatus.OK);
	}
}
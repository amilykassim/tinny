package com.oltranz.consumer;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import com.oltranz.model.GateWayModel;
import com.oltranz.model.ResponseModel;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;

@Component
public class SimpleConsumer {

	@KafkaListener(topics = "${kafka.request.topic}", groupId = "${kafka.group.id}")
	@SendTo
	public ResponseModel handle(GateWayModel request) {
		System.out.println("Attach the response...");
		
		ResponseModel response = new ResponseModel();

		// THIS IS SIMPLY FOR TESTING THE RESPONSE
		response.setAppDrivenMenuCode(request.getSessionId());
		response.setApplicationResponse(request.getphoneNumber());
		return response;
	}
}
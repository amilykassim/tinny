package com.oltranz.config;

import com.oltranz.model.ResponseModel;
import com.oltranz.model.GateWayModel;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;

@Configuration
public class KafkaConfig {

	@Value("${kafka.group.id}")
	private String groupId;

	@Value("${kafka.reply.topic}")
	private String replyTopic;

	@Bean
	public ReplyingKafkaTemplate<String, GateWayModel, ResponseModel> replyingKafkaTemplate(ProducerFactory<String, GateWayModel> pf,
			ConcurrentKafkaListenerContainerFactory<String, ResponseModel> factory) {
		ConcurrentMessageListenerContainer<String, ResponseModel> replyContainer = factory.createContainer(replyTopic);
		replyContainer.getContainerProperties().setMissingTopicsFatal(false);
		replyContainer.getContainerProperties().setGroupId(groupId);
		return new ReplyingKafkaTemplate<>(pf, replyContainer);
	}

	@Bean
	public KafkaTemplate<String, ResponseModel> replyTemplate(ProducerFactory<String, ResponseModel> pf,
			ConcurrentKafkaListenerContainerFactory<String, ResponseModel> factory) {
		KafkaTemplate<String, ResponseModel> kafkaTemplate = new KafkaTemplate<>(pf);
		factory.getContainerProperties().setMissingTopicsFatal(false);
		factory.setReplyTemplate(kafkaTemplate);
		return kafkaTemplate;
	}
}
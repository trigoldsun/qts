package com.qts.biz.settle.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KafkaConfigTest {

    @Test
    void settleTasksTopic_CreatesCorrectTopicName() {
        KafkaConfig kafkaConfig = new KafkaConfig();
        
        NewTopic topic = kafkaConfig.settleTasksTopic();
        
        assertNotNull(topic);
        assertEquals("qts.settle.tasks", topic.name());
    }

    @Test
    void settleTasksTopic_HasCorrectPartitions() {
        KafkaConfig kafkaConfig = new KafkaConfig();
        
        NewTopic topic = kafkaConfig.settleTasksTopic();
        
        assertEquals(4, topic.numPartitions());
    }

    @Test
    void topicBuild_CreatesValidTopic() {
        NewTopic topic = org.springframework.kafka.config.TopicBuilder.name("test.topic")
                .partitions(3)
                .replicas(1)
                .build();
        
        assertEquals("test.topic", topic.name());
        assertEquals(3, topic.numPartitions());
    }
}
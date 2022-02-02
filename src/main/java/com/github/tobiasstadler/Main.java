package com.github.tobiasstadler;

import co.elastic.apm.api.ElasticApm;
import co.elastic.apm.api.Scope;
import co.elastic.apm.api.Transaction;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

import static java.util.Collections.singletonList;

public class Main {

    public static void main(String[] args) {
        if (args[0].equals("producer")) {
            Properties props = new Properties();
            props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, System.getenv("KAFKA_BOOTSTRAP_SERVER"));
            props.put(ProducerConfig.CLIENT_ID_CONFIG, System.getenv("ELASTIC_APM_SERVICE_NAME"));
            props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class.getName());
            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            KafkaProducer<Long, String> producer = new KafkaProducer<>(props);

            for (long i = 0; i < 1000; ++i) {
                Transaction transaction = ElasticApm.startTransaction().setName("Sending from " + System.getenv("ELASTIC_APM_SERVICE_NAME"));
                try (Scope scope = transaction.activate()) {
                    producer.send(new ProducerRecord<>("topic", i, "payload"));
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignored) {
                        Thread.interrupted();
                    }
                } finally {
                    transaction.end();
                }
            }
            producer.close();
        } else if (args[0].equals("consumer")) {
            Properties props = new Properties();
            props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, System.getenv("KAFKA_BOOTSTRAP_SERVER"));
            props.put(ConsumerConfig.GROUP_ID_CONFIG, System.getenv("ELASTIC_APM_SERVICE_NAME"));
            props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
            props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1);
            props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class.getName());
            props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            KafkaConsumer<Long, String> consumer = new KafkaConsumer<>(props);
            consumer.subscribe(singletonList("topic"));

            for (long i = 0; i < 1000; ++i) {
                ConsumerRecords<Long, String> consumerRecords = consumer.poll(1000);
                consumerRecords.forEach(record -> {
                    System.out.println(record.key());
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignored) {
                        Thread.interrupted();
                    }
                });
                consumer.commitAsync();
            }
            consumer.close();
        }
    }
}

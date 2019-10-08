package no.nav.kafkacodelab;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

public class DiceRollConsumer {
    private static LongAdder rollsSeen = new LongAdder();
    private static final Logger LOGGER = LogManager.getLogger(DiceRollConsumer.class);
    public static void main(String[] args) {
        DiceRollConsumer consumer = new DiceRollConsumer();
        consumer.start(consumer.logAndCount());
    }

    private Consumer<ConsumerRecord<DiceCount, DiceRoll>> logAndCount() {
        return r ->  {
            LOGGER.info("key: {} , value: {}", r.key(), r.value());
            rollsSeen.increment();
        };
    }

    private void start(Consumer<ConsumerRecord<DiceCount, DiceRoll>> onMessage) {
        try(KafkaConsumer<DiceCount, DiceRoll> consumer = new KafkaConsumer<>(getConfig())) {
            consumer.subscribe(List.of("dice-rolls"));

            while(true) {
                //To read messages from a Kafka topic, use the consumer.poll(Duration) method
                // For instance consumer.poll(Duration.ofMillis(100))
                // The duration is for how long the consumer waits for a batch to fill up
                // If the batch is not filled within the duration given, the poll call will return
                // with the amount of messages received so far
                // The poll call returns a ConsumerRecords class, which supports normal iteration methods to get at
                // the ConsumerRecord(s) contained within
            }
        }
    }

    /**
     * Kafka uses this to remember how far you've read, so if you want it to be as though Kafka never seen you,
     * add some randomness to this method
     * @return
     */
    private String getGroupId() {
        return "my-groupid";
    }

    private Properties getConfig() {
        Properties p = new Properties();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        p.put(ConsumerConfig.GROUP_ID_CONFIG, getGroupId());
        p.put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081");
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        p.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
        p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return p;
    }
}

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
                ConsumerRecords<DiceCount, DiceRoll> records = consumer.poll(Duration.ofMillis(1000));
                records.forEach(onMessage);
                LOGGER.debug("Seen {} rolls so far", rollsSeen.longValue());
            }
        }
    }

    private Properties getConfig() {
        Properties p = new Properties();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        p.put(ConsumerConfig.GROUP_ID_CONFIG, "my-rolls3");
        p.put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081");
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        p.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
        p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return p;
    }
}

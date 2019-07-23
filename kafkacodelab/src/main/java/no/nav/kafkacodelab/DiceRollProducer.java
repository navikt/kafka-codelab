package no.nav.kafkacodelab;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

public class DiceRollProducer {
    private static final Logger LOGGER = LogManager.getLogger(DiceRollProducer.class);
    public static void main(String[] args) {
        DiceRollProducer producer = new DiceRollProducer();

        int numOfRolls = 100;
        if (args.length > 0) {
            numOfRolls = Integer.parseInt(args[0]);
        }
        producer.startRolling(numOfRolls);
    }

    private void startRolling(int numberOfRolls) {
        Random r = new Random();
        try (KafkaProducer<DiceCount, DiceRoll> producer = new KafkaProducer<>(getConfig())) {
            for (int roll = 0; roll < numberOfRolls; roll++) {
                int count = r.nextInt(5) + 1; // Roll anywhere between 1 and 5 dice
                List<Integer> dice = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    dice.add(r.nextInt(6) + 1);
                }
                DiceRoll diceRoll = DiceRoll.newBuilder().setCount(count).setDice(dice).build();
                DiceCount diceCount = DiceCount.newBuilder().setCount(count).build();
                LOGGER.info("Rolled {}", diceRoll);
                producer.send(new ProducerRecord<>("rolls", diceCount, diceRoll));
            }
        }
    }

    private Properties getConfig() {
        Properties p = new Properties();
        p.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        p.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081");
        p.put(ProducerConfig.CLIENT_ID_CONFIG, "diceroller-mine");
        p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        return p;
    }
}

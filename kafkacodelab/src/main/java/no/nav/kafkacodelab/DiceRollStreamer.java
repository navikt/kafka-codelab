package no.nav.kafkacodelab;

import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Properties;
import java.util.Set;

public class DiceRollStreamer {
    private static final Logger LOGGER = LogManager.getLogger(DiceRollStreamer.class);

    public static void main(String[] args) {
        StreamsBuilder streamsBuilder = new StreamsBuilder();
        KStream<DiceCount, DiceRoll> rolls = streamsBuilder
                .stream("dice-rolls");
        // @TODO Use the branch method to create a branch for each diceCount
        // The branch method accepts a varying number of lambda predicates to select which messages
        // should go into the branch. So if you only want a branch of rolls with 5 dice you could do
        // .branch((k, v) -> k.getCount() == 5) which would give you a KStream array of one element
        // consisting of all messages with count == 5
        KStream<DiceCount, DiceRoll>[] counts = rolls.branch();
        for (int i = 0; i < counts.length; i++) {
            int diceCount = (5 - i);
            counts[i].to("dice-rolls-" + diceCount);
            counts[i].map((k, r) -> KeyValue.pair(k.getCount(), r.getDice().stream().mapToInt(j -> j).sum()))
                    .to("dice-rolls-sum-" +diceCount, Produced.with(Serdes.Integer(), Serdes.Integer()));
        }
        // @TODO Use the filter method to pick out yatzy rolls - That is one where all 5 dice are the same


        KafkaStreams streams = new KafkaStreams(streamsBuilder.build(), getConfig());
        streams.start();
    }

    private static Properties getConfig() {
        Properties p = new Properties();
        p.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        p.put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081");
        p.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, SpecificAvroSerde.class);
        p.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, SpecificAvroSerde.class);
        p.put(StreamsConfig.APPLICATION_ID_CONFIG, "my-application-id");
        p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return p;
    }
}

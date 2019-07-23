# Kafka introduction

## Overview of the tutorial

This tutorial shows you how to create a Kafka-consumer and -producer using [kafka-clients](http://kafka.apache.org/documentation/#api) java library.

What you'll learn
* How to create a Kafka producer that produce data from a Kafka-broker
* How to create a Kafka consumer that reads data from a Kafka-broker
* How to create a Kafka streams application that performs computations on input from one topic and 
 sends output to one or more other topics

Positive
: This tutorial requires that you are familiar with Java programming language.

### Prerequisites

* Internet connectivity
* Java SDK installed - see [https://www.oracle.com/technetwork/java/javase/downloads/index.html](https://www.oracle.com/technetwork/java/javase/downloads/index.html) for instructions.
* Docker - see [https://www.docker.com/get-started](https://www.docker.com/get-started) for instructions. NB! Docker on Windows VDI requires virtualization, see [Docker on Windows VDI requires virtualization](#Docker-on-Windows-VDI-requires-virtualization)
* IntelliJ IDEA installed. Choose the Community version from the download link [IntelliJ Idea](https://www.jetbrains.com/idea/download)
* Git installed - see [https://git-scm.com/](https://git-scm.com/) for instructions.


#### Docker on Windows VDI requires virtualization 
* To be able to run the Docker daemon that comes with Docker Desktop (formerly known as Docker For Windows) in Nav's Windows virtual machine images we need to enable virtualization (Hyper-V) in the image.
* You can ask to have virtualization enabled in the Slack channel #tech_virtualisering. You need to provide the name of the virtual machine, which you will find from within the image > Windows > Kontrollpanel > System og sikkerhet > System. Enabling virtualization requires reboot of the image.
* Verify by opening Windows > Aktiver eller deaktiver Windows-funksjoner, find "Hyper-V" and see that all it's checkboxes are enabled.
 

## Set up
1. Clone the https://github.com/navikt/kafka-codelab.git repository
```@bash
git clone https://github.com/navikt/kafka-codelab.git
```

2. Go to the kafkacodelabschema project in the kafka-codelab repository and run a clean install from a terminal
```@bash
mvn clean install
```

3. Open a terminal window and run the landoop/fast-data-dev docker image
```@bash
docker run --rm -p 2181:2181 -p 3030:3030 -p 8081-8083:8081-8083 \
       -p 9581-9585:9581-9585 -p 9092:9092 -e ADV_HOST=localhost \
       landoop/fast-data-dev:latest
```

4. Optional: Create a 'dice-rolls' topic
```@bash
docker run --rm -it --net=host landoop/fast-data-dev kafka-topics --zookeeper localhost:2181 \
       --create --topic dice-rolls --replication-factor 1 --partitions 1
```

5. Optional: Visit http://localhost:3030/kafka-topics-ui/#/ to verify that the 'dice-rolls' topic was created

### Assignment

1. Create a kafka producer which produces messages to the 'dice-rolls' topic
    - You can either create your own producer class from scratch or base it on the example below
    - If you get stuck there is a proposed solution in the kafkacodelab project (no.nav.kafkacodelab.DiceRollProducer)

```@java
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
        // @todo: Create a kafka producer
        // @todo: Use the rollDices() method and produce a kafka message for every dice roll to the 'dice-rolls' topic
    }

    private AbstractMap.SimpleEntry<DiceCount, DiceRoll> rollDices() {
        Random r = new Random();
        int count = r.nextInt(5) + 1; // Roll anywhere between 1 and 5 dice
        List<Integer> dice = getRollResult(r, count);
        DiceRoll diceRoll = DiceRoll.newBuilder().setCount(count).setDice(dice).build();
        DiceCount diceCount = DiceCount.newBuilder().setCount(count).build();
        LOGGER.info("Rolled {}", diceRoll);
        return new AbstractMap.SimpleEntry<>(diceCount, diceRoll);
    }

    private List<Integer> getRollResult(Random r, int count) {
        List<Integer> dice = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            dice.add(r.nextInt(6) + 1);
        }
        return dice;
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
```

2. Create a kafka consumer which consumes the 'dice-rolls' topic and prints each message received to console.
    - You can either create your own consumer class from scratch or base it on the example below
    - If you get stuck there is a proposed solution in the kafkacodelab project (no.nav.kafkacodelab.DiceRollConsumer)
    
```@java
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
        // @todo: Create a kafka consumer
        // @todo: Consume the 'roll-dices' topic and print each message received to console
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
```

3. Create a kafka streams application
    - The streams application shall:
        - create a separate topic containing dice rolls for every number of dices rolled (1->5)
        - create a new topic containing all the true yatzy rolls (5 dices, all dices with same number)
    - You can either create your own kafka stream class from scratch or base it on the example below
    - If you get stuck there is a proposed solution in the kafkacodelab project (no.nav.kafkacodelab.DiceRollStreamer)
    - Go to [kafka-streams](https://kafka.apache.org/documentation/streams/) for additional information on kafka streams
    
```@java
public class DiceRollStreamer {
    private static final Logger LOGGER = LogManager.getLogger(DiceRollStreamer.class);

    public static void main(String[] args) {
        // @todo: Create a kafka stream consuming the 'roll-dices' topic
        // @todo: Create five new topics containing dice rolls for the different number of dices rolled (1->5)
        // @todo: Create a new topic containing all the true yatzy rolls (5 dices, with all 5 dices with same number)
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
```
import requests
from requests.exceptions import HTTPError
import avro
import avro.schema
import avro.io
import io
import time
import json
import struct
import random
from kafka import KafkaProducer


schema_cache = {}
id_cache = {}


def getSchemaForValue():
    with open("../kafkacodelabschema/src/main/avro/dice-roll.avsc") as schema:
        return json.load(schema)


def getSchemaForKey():
    with open("../kafkacodelabschema/src/main/avro/dice-count.avsc") as schema:
        return json.load(schema)


def writeMessageWithId(schema_name, message):
    schema = avro.schema.Parse(schema_cache[schema_name])
    writer = avro.io.DatumWriter(schema)
    bytes_writer = io.BytesIO()
    encoder = avro.io.BinaryEncoder(bytes_writer)
    bytes_writer.write(bytes([0]))
    bytes_writer.write(id_cache[schema_name].to_bytes(4, byteorder="big"))
    writer.write(message, encoder)
    return bytes_writer.getvalue()


def register_schema(schema_name, schema):
    try:
        postData = {
            "schema": schema[0]
        }
        response = requests.post(
            f'http://localhost:8081/subjects/{schema_name}/versions',
            data=json.dumps(postData),
            headers={"Content-Type": "application/vnd.schemaregistry.v1+json"})
        response.raise_for_status()
    except Exception as err:
        print(f"Failed to register schema {err}")
    else:
        schema_cache[schema_name] = schema[0]
        id_cache[schema_name] = response.json()["id"]


def fetch_schema(schema_name, version):
    response = requests.get(
        f'http://localhost:8081/subjects/{schema_name}/versions/{version}').json()
    schema_cache[schema_name] = response["schema"]
    id_cache[schema_name] = response["id"]


def register(schema_name, schema):
    if schema_name in schema_cache:
        schema_cache[schema_name]
    else:
        response = requests.get(
            f'http://localhost:8081/subjects/{schema_name}/versions')
        if response.status_code == 200:
            versions = response.json()
            newestVersion = max(versions)
            fetch_schema(schema_name, newestVersion)
        elif response.status_code == 404:
            register_schema(schema_name, schema)
        else:
            print("Failed to find or register schema")


topicName = "dice-rolls2"
producer = KafkaProducer(bootstrap_servers='localhost:9092')
keySchema = getSchemaForKey()
valueSchema = getSchemaForValue()
keyId = register(f'{topicName}-key', keySchema)
valueId = register(f'{topicName}-value', valueSchema)


def produce_message(diceCount, diceRoll):
    producer.send(topic=topicName,
                  key=writeMessageWithId(f'{topicName}-key', diceCount),
                  value=writeMessageWithId(f'{topicName}-value', diceRoll))


def buildDieCount(die_count):
    return {"count": die_count}


def buildDiceRoll(diceRolls):
    return {"count": len(diceRolls), "dice": diceRolls}


def generate_dice_rolls(count=100):
    for roll in range(0, count):
        dies = [random.randrange(1, 6)
                for r in range(0, random.randrange(1, 5))]
        key = buildDieCount(len(dies))
        value = buildDiceRoll(dies)
        produce_message(key, value)


generate_dice_rolls(count=5)

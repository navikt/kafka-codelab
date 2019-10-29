import requests
import avro
import avro.schema
import avro.io
import io
import time
import json
import struct
from kafka import KafkaConsumer

schema_cache = {}

consumer = KafkaConsumer("dice-rolls", group_id='workshop3',
                         bootstrap_servers='localhost:9092', auto_offset_reset='earliest')


def _parse_kafka_message(message):
    try:
        schema_res = _get_schema_from_registry(message=message)
        schema = schema_res.json()["schema"]
    except (AttributeError, KeyError):
        return json.loads(message.value.decode('utf8'))
    else:
        return _decode_avro_message(schema=schema, message=message)


def _get_schema_from_registry(message):
    schema_id = struct.unpack(">L", message.value[1:5])[0]
    if schema_id in schema_cache:
        return schema_cache[schema_id]
    else:
        schema = requests.get(
            'http://localhost:8081/schemas/ids/' + str(schema_id))
        schema_cache[schema_id] = schema
        return schema


def _decode_avro_message(schema, message):
    pschema = avro.schema.Parse(schema)
    bytes_reader = io.BytesIO(message.value[5:])
    decoder = avro.io.BinaryDecoder(bytes_reader)
    reader = avro.io.DatumReader(pschema)
    return reader.read(decoder)


for msg in consumer:
    m = _parse_kafka_message(msg)
    print(m)

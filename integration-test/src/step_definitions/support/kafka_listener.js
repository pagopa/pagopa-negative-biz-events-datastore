var Kafka = require('node-rdkafka');

const namespace = process.env.EVENT_HUB_NAMESPACE_AWAKABLE_FINAL;
function createKafkaStream(eventHubName, connectionString) {
    return stream = Kafka.KafkaConsumer.createReadStream({
        'metadata.broker.list': namespace + '.servicebus.windows.net:9093',
        'group.id': 'nodejs-cg', //The default consumer group for EventHubs is $Default
        'socket.keepalive.enable': true,
        'enable.auto.commit': false,
        'security.protocol': 'SASL_SSL',
        'sasl.mechanisms': 'PLAIN',
        'sasl.username': '$ConnectionString', //do not replace $ConnectionString
        'sasl.password': connectionString,
      }, {}, {
        topics: eventHubName,
        waitInterval: 0,
        objectMode: false
      });
}

module.exports = {
    createKafkaStream
}
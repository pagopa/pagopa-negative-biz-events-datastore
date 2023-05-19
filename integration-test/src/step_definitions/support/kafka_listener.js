const kafka = require('node-rdkafka');
const {sleep} = require("./common");

const namespace = process.env.EVENT_HUB_NAMESPACE_AWAKABLE_FINAL;
function createKafkaStream(eventHubName, connectionString) {
    return stream = kafka.KafkaConsumer.createReadStream({
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

async function listenerMultipleInsertion(isAwakable, multipleInsertion, totalMessages, start, numEvents){
  if(isAwakable){
    var stream = (createKafkaStream(process.env.EVENT_HUB_NAME_AWAKABLE, process.env.EVENT_HUB_AWAKABLE_RX_CONNECTION_STRING));
  } else {
    var stream = (createKafkaStream(process.env.EVENT_HUB_NAME_FINAL, process.env.EVENT_HUB_FINAL_RX_CONNECTION_STRING));
  }
  stream.consumer.on('data', (message) => {totalMessages.push(JSON.parse(message.value.toString()))});
  await sleep(15000);
  await multipleInsertion(start, numEvents, isAwakable);
  stream.destroy();
}

module.exports = {
    createKafkaStream, listenerMultipleInsertion
}
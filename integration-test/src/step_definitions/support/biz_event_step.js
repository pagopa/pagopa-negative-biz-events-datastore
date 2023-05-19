const assert = require('assert');
const {createNegativeBizEvent, sleep, awakableCaseHandling} = require("./common");
const {publishEvent} = require("./event_hub_client");
const {getDocumentById, createDocument, deleteDocument, multipleInsertion} = require("./datastore_client");
const {After, Given, When, Then, setDefaultTimeout} = require('@cucumber/cucumber');
const {createKafkaStream, listenerMultipleInsertion} = require("./kafka_listener");

let eventId;

let parsedMessage;

let totalMessages = new Array();

setDefaultTimeout(360 * 1000);

//After each Scenario
After(function () {
    // remove event
    deleteDocument(eventId)
    let totalInsertion = 11;
    for(let i = 0; i <= totalInsertion; i++){
      deleteDocument("test-id" + String(i));
    }
});

// Given

Given('a random {string} biz event with id {string} published on eventhub', async function (type, id) {
    // prior cancellation to avoid dirty cases
    await deleteDocument(id);
    eventId = id;
    let isAwakable = awakableCaseHandling(type);

    const event = createNegativeBizEvent(eventId, isAwakable);
    let responseToCheck =  await publishEvent(event);

    assert.strictEqual(responseToCheck.status, 201);
});

Given('a random {string} biz event with id {string}', async function (type, id) {
      let isAwakable = awakableCaseHandling(type);
      parsedMessage = null;
      var stream = (type === 'final') ? 
                    (createKafkaStream(process.env.EVENT_HUB_NAME_FINAL, process.env.EVENT_HUB_FINAL_RX_CONNECTION_STRING)) : 
                    (createKafkaStream(process.env.EVENT_HUB_NAME_AWAKABLE, process.env.EVENT_HUB_AWAKABLE_RX_CONNECTION_STRING));
      stream.consumer.on('data', (message) => {parsedMessage = JSON.parse(message.value.toString())});
      await sleep(10000);
      
      // prior cancellation to avoid dirty cases
      await deleteDocument(id);
      eventId = id;

      let responseToCheck =  await createDocument(id, isAwakable);
      assert.strictEqual(responseToCheck.status, 201);
});

Given('{int} random awakable and {int} final biz events', async function (numAwakable, numFinal) {
      await listenerMultipleInsertion(false, multipleInsertion, totalMessages, 0, numFinal);
      await listenerMultipleInsertion(true, multipleInsertion, totalMessages, numFinal, numAwakable + numFinal);
});

// When
When('biz event has been properly stored into datastore after {int} ms', async function (time) {
    // boundary time spent by azure function to process event
    await sleep(time);
});

// Then
Then('the datastore returns the event with id {string}', async function (targetId) {
    responseToCheck = await getDocumentById(targetId);
    assert.strictEqual(responseToCheck.data.Documents[0].id, targetId);
});

Then('the eventhub retrieves the event with id {string}', async function (targetId) {
    stream.destroy();
    assert.strictEqual(parsedMessage.id, targetId);
});

Then('the eventhub retrieves the {int} awakable and {int} final events', async function (numAwakable, numFinal) {
  let counterAwakable = 0, counterFinal = 0;
  for(let i = 0; i < totalMessages.length; i++){
    if(totalMessages[i].id.startsWith('test-id')){
      if(totalMessages[i].reAwakable === true){
        counterAwakable++;
      } else if(totalMessages[i].reAwakable === false){
        counterFinal++;
      }
    } 
  }
  assert.strictEqual(numAwakable, counterAwakable);
  assert.strictEqual(numFinal, counterFinal);
});
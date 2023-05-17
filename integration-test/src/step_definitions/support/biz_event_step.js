const assert = require('assert');
const {createNegativeBizEvent, sleep} = require("./common");
const {publishEvent} = require("./event_hub_client");
const {getDocumentById, createDocument, deleteDocument} = require("./datastore_client");
const {After, Given, When, Then, setDefaultTimeout} = require('@cucumber/cucumber');
const {createKafkaStream} = require("./kafka_listener");

let eventId;

let parsedMessage;

setDefaultTimeout(60 * 1000);

//After each Scenario
After(function () {
    // remove event
    deleteDocument(eventId)
});

// Given

Given('a random {string} biz event with id {string} published on eventhub', async function (type, id) {
    // prior cancellation to avoid dirty cases
    await deleteDocument(id);
    eventId = id;
    let isAwakable = false;

    switch (type) {
        case 'final':
          isAwakable = false;
          break;
        case 'awakable':
          isAwakable = true;
          break;
        default:
          isAwakable = false;
      }


    const event = createNegativeBizEvent(eventId, isAwakable);
    let responseToCheck =  await publishEvent(event);

    assert.strictEqual(responseToCheck.status, 201);
});

Given('a random {string} biz event with id {string}', async function (type, id) {
      let isAwakable = false;
      parsedMessage = null;
      switch (type) {
        case 'final':
          isAwakable = false;
          var stream = (createKafkaStream(process.env.EVENT_HUB_NAME_FINAL, process.env.EVENT_HUB_FINAL_RX_CONNECTION_STRING));
          break;
        case 'awakable':
          isAwakable = true;
          var stream = (createKafkaStream(process.env.EVENT_HUB_NAME_AWAKABLE, process.env.EVENT_HUB_AWAKABLE_RX_CONNECTION_STRING));
          break;
        default:
          isAwakable = false;
      }
      stream.consumer.on('data', (message) => {parsedMessage = JSON.parse(message.value.toString())});
      await sleep(10000);
      
      // prior cancellation to avoid dirty cases
      await deleteDocument(id);
      eventId = id;

      let responseToCheck =  await createDocument(id, isAwakable);
      assert.strictEqual(responseToCheck.status, 201);
});

Given('{int} random awakable and {int} final biz events', async function (numAwakable, numFinal) {
      let totalMessages = new Array();
      var streamFinal = (createKafkaStream(process.env.EVENT_HUB_NAME_FINAL, process.env.EVENT_HUB_FINAL_RX_CONNECTION_STRING));
      var streamAwakable = (createKafkaStream(process.env.EVENT_HUB_NAME_AWAKABLE, process.env.EVENT_HUB_AWAKABLE_RX_CONNECTION_STRING));
      streamFinal.consumer.on('data', (message) => {totalMessages.push(JSON.parse(message.value.toString()))});
      streamAwakable.consumer.on('data', (message) => {totalMessages.push(JSON.parse(message.value.toString()))});
      await sleep(10000);

      for (let i = 0; i < numAwakable + numFinal; i++) {
        await deleteDocument(String(i));
        let responseToCheck =  await createDocument(String(i), isAwakable);
        assert.strictEqual(responseToCheck.status, 201);
      }

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
  streamFinal.destroy();
  streamAwakable.destroy();
  let counterAwakable = 0, counterFinal = 0;
  for(let i = 0; i < totalMessages.length(); i++){
    if(totalMessages[i].reAwakable === "awakable"){
      counterAwakable++;
    } else if(totalMessages[i].reAwakable === "final"){
      counterFinal++;
    }
  }
  assert.strictEqual(numAwakable, counterAwakable);
  assert.strictEqual(numFinal, counterFinal);
});
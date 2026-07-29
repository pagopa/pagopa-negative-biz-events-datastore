const assert = require('assert');
const {createNegativeBizEvent, sleep, awakableCaseHandling} = require("./common");
const {publishEvent} = require("./event_hub_client");
const {getDocumentById, createDocument, deleteDocument, multipleInsertion} = require("./datastore_client");
const {After, Given, When, Then, setDefaultTimeout} = require('@cucumber/cucumber');
const {createKafkaStream, listenerMultipleInsertion} = require("./kafka_listener");
const {makeIdMix, makeIdNumber} = require("./utility/helpers")

let eventId;

let parsedMessage;

let totalMessages = new Array();

let eventCreationTimestamp;
let expectedEvent;

function removeCosmosSystemFields(event, isRoot = true) {
    if (Array.isArray(event)) {
        return event.map(item => removeCosmosSystemFields(item, false));
    }
    if (event !== null && typeof event === 'object') {
        const sanitized = {};
        for (const [key, value] of Object.entries(event)) {
            // strip Cosmos system fields from root document
            if (isRoot && ['_rid', '_self', '_etag', '_attachments', '_ts'].includes(key)) continue;
            // `properties` is cleared by the service on storage: preserve the key as empty object
            if (isRoot && key === 'properties') { sanitized[key] = {}; continue; }
            // Cosmos DB reserves `id` at document level and removes it from nested objects
            if (!isRoot && key === 'id') continue;
            sanitized[key] = removeCosmosSystemFields(value, false);
        }
        return sanitized;
    }
    return event;
}

function assertExpectedStructure(actual, expected, path) {
    if (Array.isArray(expected)) {
        assert.ok(Array.isArray(actual), `Field ${path} should be an array`);
        assert.strictEqual(actual.length, expected.length, `Unexpected length for array ${path}`);
        expected.forEach((expectedItem, index) => {
            assertExpectedStructure(actual[index], expectedItem, `${path}[${index}]`);
        });
        return;
    }

    if (expected !== null && typeof expected === 'object') {
        assert.ok(actual !== null && typeof actual === 'object', `Field ${path} should be an object`);
        Object.keys(expected).forEach((key) => {
            assert.ok(Object.prototype.hasOwnProperty.call(actual, key), `Missing field ${path}.${key}`);
            assertExpectedStructure(actual[key], expected[key], `${path}.${key}`);
        });
        return;
    }

    assert.strictEqual(actual, expected, `Unexpected value for field ${path}`);
}

setDefaultTimeout(2 * 60 * 1000);

//After each Scenario
After(function () {
    // remove event
    deleteDocument(eventId)
});

// Given

Given('a random {string} biz event is published on eventhub', async function (type) {
	eventId = makeIdMix(15);
  
    let isAwakable = awakableCaseHandling(type);

    const event = createNegativeBizEvent(eventId, isAwakable);
    expectedEvent = removeCosmosSystemFields(event);
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

// When
When('the eventhub sends the same {string} biz event again', async function (type) {
    let isAwakable = awakableCaseHandling(type);

    const event = createNegativeBizEvent(eventId, isAwakable);
    let responseToCheck =  await publishEvent(event);

    assert.strictEqual(responseToCheck.status, 201);
});


// Then
Then('the datastore returns the event', async function () {
    responseToCheck = await getDocumentById(eventId);
    eventCreationTimestamp = responseToCheck.data.Documents[0]._ts;
    assert.strictEqual(responseToCheck.data.Documents[0].id, eventId);
});

Then('the eventhub retrieves the event with id {string}', async function (targetId) {
    stream.destroy();
    assert.strictEqual(parsedMessage.id, targetId);
});

Then('the eventhub retrieves at least the {int} awakable and {int} final events', async function (numAwakable, numFinal) {
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
  assert.ok(counterAwakable >= numAwakable);
  assert.ok(counterFinal >= numFinal);
});

Then('the datastore returns the event with all expected fields', async function () {
    responseToCheck = await getDocumentById(eventId);
    assert.strictEqual(responseToCheck.status, 200, `Unexpected Cosmos response: ${JSON.stringify(responseToCheck && responseToCheck.data)}`);
    assert.ok(responseToCheck.data && Array.isArray(responseToCheck.data.Documents), `Cosmos response has no Documents array: ${JSON.stringify(responseToCheck && responseToCheck.data)}`);
    assert.ok(responseToCheck.data.Documents.length > 0, `Document with id ${eventId} not found`);
    const doc = responseToCheck.data.Documents[0];
    assertExpectedStructure(doc, expectedEvent, 'document');
});

Then('the datastore returns the not updated event', async function () {
    responseToCheck = await getDocumentById(eventId);
    assert.strictEqual(responseToCheck.status, 200, `Unexpected Cosmos response: ${JSON.stringify(responseToCheck && responseToCheck.data)}`);
    assert.ok(responseToCheck.data && Array.isArray(responseToCheck.data.Documents), `Cosmos response has no Documents array: ${JSON.stringify(responseToCheck && responseToCheck.data)}`);
    assert.ok(responseToCheck.data.Documents.length > 0, `Document with id ${eventId} not found`);
    assert.strictEqual(responseToCheck.data.Documents[0].id, eventId);
    assert.strictEqual(responseToCheck.data.Documents[0]._ts, eventCreationTimestamp);
});
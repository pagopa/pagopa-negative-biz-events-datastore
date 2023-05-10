const assert = require('assert');
const {createNegativeBizEvent, sleep} = require("./common");
const {publishEvent} = require("./event_hub_client");
const {getDocumentById, createDocument, deleteDocument} = require("./datastore_client");
const {After, Given, When, Then} = require('@cucumber/cucumber');

let eventId;


// After each Scenario
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

      let responseToCheck =  await createDocument(id, isAwakable);
      assert.strictEqual(responseToCheck.status, 201);
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

Then('the eventhub returns the event with id {string}', async function (targetId) {
  responseToCheck = await getDocumentById(targetId);
  assert.strictEqual(responseToCheck.data.Documents[0].id, targetId);
});

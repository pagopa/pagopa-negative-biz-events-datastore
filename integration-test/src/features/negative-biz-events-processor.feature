Feature: All about payment events consumed by Azure Function biz-event-processor

  Scenario: A final negative biz event published on Event-Hub is stored into datastore
    Given a random "final" biz event is published on eventhub
    When biz event has been properly stored into datastore after 20000 ms
    Then the datastore returns the event

  Scenario: An awakable negative biz event published on Event-Hub is stored into datastore
    Given a random "awakable" biz event is published on eventhub
    When biz event has been properly stored into datastore after 20000 ms
    Then the datastore returns the event
    
  Scenario: A final negative biz event published on Event-Hub is skipped by cache
    Given a random "final" biz event is published on eventhub
    When biz event has been properly stored into datastore after 20000 ms
    Then the datastore returns the event
    When the eventhub sends the same "final" biz event again
    Then the datastore returns the not updated event
    
  Scenario: An awakable negative biz event published on Event-Hub is skipped by cache
    Given a random "awakable" biz event is published on eventhub
    When biz event has been properly stored into datastore after 20000 ms
    Then the datastore returns the event
    When the eventhub sends the same "awakable" biz event again
    Then the datastore returns the not updated event

  Scenario: A final negative biz event stored into datastore is published on Event-Hub
    Given a random "final" biz event with id "test-id-3"
    When biz event has been properly stored into datastore after 20000 ms
    Then the eventhub retrieves the event with id "test-id-3"

  Scenario: A awakable negative biz event stored into datastore is published on Event-Hub
    Given a random "awakable" biz event with id "test-id-3"
    When biz event has been properly stored into datastore after 20000 ms
    Then the eventhub retrieves the event with id "test-id-3"

  Scenario: A list of awakable and final negative biz events stored into datastore are published on Event-Hub
    Given 2 random awakable and 3 final biz events
    When biz event has been properly stored into datastore after 20000 ms
    Then the eventhub retrieves at least the 2 awakable and 3 final events
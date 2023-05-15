Feature: All about payment events consumed by Azure Function biz-event-processor

  Scenario: A final negative biz event published on Event-Hub is stored into datastore
    Given a random "final" biz event with id "test-id-3" published on eventhub
    When biz event has been properly stored into datastore after 2000 ms
    Then the datastore returns the event with id "test-id-3"

  Scenario: An awakable negative biz event published on Event-Hub is stored into datastore
    Given a random "awakable" biz event with id "test-id-3" published on eventhub
    When biz event has been properly stored into datastore after 2000 ms
    Then the datastore returns the event with id "test-id-3"
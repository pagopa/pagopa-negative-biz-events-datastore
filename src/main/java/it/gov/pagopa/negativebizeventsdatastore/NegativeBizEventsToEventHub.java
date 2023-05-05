package it.gov.pagopa.negativebizeventsdatastore;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.OutputBinding;
import com.microsoft.azure.functions.annotation.CosmosDBTrigger;
import com.microsoft.azure.functions.annotation.EventHubOutput;
import com.microsoft.azure.functions.annotation.FunctionName;

import it.gov.pagopa.negativebizeventsdatastore.entity.BizEvent;


public class NegativeBizEventsToEventHub {

  @FunctionName("NegativeBizEventsToEventHubProcessor")
  public void processNegativeBizEventsToEventHub(
      @CosmosDBTrigger(
          name = "NegativeBizEventsDatastore",
          databaseName = "db",
          containerName = "negative-biz-events",
          leaseContainerName = "negative-biz-events-leases",
          createLeaseContainerIfNotExists = true,
          maxItemsPerInvocation=100,
          connection = "COSMOS_CONN_STRING") 
      List<BizEvent> items,
      @EventHubOutput(
              name = "FinalNegativeBizEventsHub",
              eventHubName = "", // blank because the value is included in the connection string
              connection = "FINAL_EVENTHUB_CONN_STRING")
      OutputBinding<List<BizEvent>> finalEvtMsg,
      @EventHubOutput(
          name = "AwakableNegativeBizEventsHub", 
          eventHubName = "", // blank because the value is included in the connection string
          connection = "FINAL_EVENTHUB_CONN_STRING")
      OutputBinding<List<BizEvent>> awakableEvtMsg,

      final ExecutionContext context
      ) {

    List<BizEvent> finalItems = new ArrayList<>();
    List<BizEvent> reawakableItems = new ArrayList<>();
    Logger logger = context.getLogger();

    String msg = String.format("NegativeBizEventsToEventHub stat %s function - total events triggered %d", context.getInvocationId(),  items.size());
    logger.info(msg);
    
    for (BizEvent be: items) {
      if (be.isReAwakable()) {
        reawakableItems.add(be);
      }
      else {
        finalItems.add(be);
      }
    }
    
    // call the Event Hub reawakable
    msg = String.format("NegativeBizEventsToEventHub stat %s function - number of reawakable events sent to the event hub %d", context.getInvocationId(), reawakableItems.size());
    logger.info(msg);
    awakableEvtMsg.setValue(reawakableItems);
    // call the Event Hub final
    msg = String.format("NegativeBizEventsToEventHub stat %s function - number of final events sent to the event hub %d", context.getInvocationId(), finalItems.size());
    logger.info(msg);
    finalEvtMsg.setValue(finalItems);
  }
}

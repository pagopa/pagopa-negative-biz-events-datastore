package it.gov.pagopa.negativebizeventsdatastore;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.OutputBinding;
import com.microsoft.azure.functions.annotation.BindingName;
import com.microsoft.azure.functions.annotation.Cardinality;
import com.microsoft.azure.functions.annotation.CosmosDBOutput;
import com.microsoft.azure.functions.annotation.EventHubTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;
import it.gov.pagopa.negativebizeventsdatastore.entity.BizEvent;
import it.gov.pagopa.negativebizeventsdatastore.exception.AppException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import lombok.NonNull;

/** Azure Functions with Azure Queue trigger. */
public class NegativeBizEventToDatastore {
  /** This function will be invoked when an Event Hub trigger occurs */
  @FunctionName("EventHubBizEventProcessor")
  public void processBizEvent(
      @EventHubTrigger(
              name = "NegativeBizEvent",
              eventHubName = "", // blank because the value is included in the connection string
              connection = "NEGATIVE_EVENT_HUB_CONN_STRING",
              cardinality = Cardinality.MANY)
          List<BizEvent> negativeBizEvtMsg,
      @BindingName(value = "PropertiesArray") Map<String, Object>[] properties,
      @CosmosDBOutput(
              name = "NegativeBizEventDatastore",
              databaseName = "db",
              containerName = "negative-biz-events",
              createIfNotExists = false,
              connection = "COSMOS_CONN_STRING")
          @NonNull
          OutputBinding<List<BizEvent>> documentdb,
      final ExecutionContext context) {

    Logger logger = context.getLogger();

    String message =
        String.format(
            "NegativeBizEventToDatastore function called at %s with events list size %s and"
                + " properties size %s",
            LocalDateTime.now(), negativeBizEvtMsg.size(), properties.length);
    logger.info(message);

    // persist the item
    try {
      if (negativeBizEvtMsg.size() == properties.length) {
        List<BizEvent> bizEvtMsgWithProperties = new ArrayList<>();

        for (int i = 0; i < negativeBizEvtMsg.size(); i++) {
          BizEvent be = negativeBizEvtMsg.get(i);
          be.setProperties(properties[i]);
          bizEvtMsgWithProperties.add(be);
        }

        documentdb.setValue(bizEvtMsgWithProperties);
      } else {
        throw new AppException(
            "Error during processing - The size of the events to be processed and their associated"
                + " properties does not match [bizEvtMsg.size="
                + negativeBizEvtMsg.size()
                + "; properties.length="
                + properties.length
                + "]");
      }
    } catch (Exception e) {
      logger.severe(
          "Generic exception on cosmos biz-events msg ingestion at "
              + LocalDateTime.now()
              + " : "
              + e.getMessage());
    }
  }
}

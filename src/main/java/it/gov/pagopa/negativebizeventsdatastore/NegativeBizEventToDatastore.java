package it.gov.pagopa.negativebizeventsdatastore;

import com.google.common.base.Strings;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.OutputBinding;
import com.microsoft.azure.functions.annotation.BindingName;
import com.microsoft.azure.functions.annotation.Cardinality;
import com.microsoft.azure.functions.annotation.CosmosDBOutput;
import com.microsoft.azure.functions.annotation.EventHubTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;

import it.gov.pagopa.negativebizeventsdatastore.client.RedisClient;
import it.gov.pagopa.negativebizeventsdatastore.entity.BizEvent;
import it.gov.pagopa.negativebizeventsdatastore.exception.AppException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import lombok.NonNull;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.params.SetParams;

/** Azure Functions with Azure Queue trigger. */
public class NegativeBizEventToDatastore {
	/** This function will be invoked when an Event Hub trigger occurs */

	public static final JedisPooled jedis = RedisClient.getInstance().redisConnectionFactory();

	private final int expireTimeInMS = 
			System.getenv("REDIS_EXPIRE_TIME_MS") != null ? Integer.parseInt(System.getenv("REDIS_EXPIRE_TIME_MS")) : 3600000;

	private static final String REDIS_ID_PREFIX = "negbiz_";

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
					// READ FROM THE CACHE: The cache is queried to find out if the event has already been queued --> if yes it is skipped
					String value = this.findByBizEventId(negativeBizEvtMsg.get(i).getId());
					if (Strings.isNullOrEmpty(value)) {
						BizEvent be = negativeBizEvtMsg.get(i);
						be.setProperties(properties[i]);
						// WRITE IN THE CACHE: The result of the insertion in the cache is logged to verify the correct functioning
						String result = this.saveBizEventId(negativeBizEvtMsg.get(i).getId());
						message = String.format("Negative BizEvent message with id %s was cached with result: %s",
								negativeBizEvtMsg.get(i).getId(), result);
						logger.info(message);

						bizEvtMsgWithProperties.add(be);
					} else {
						// just to track duplicate events  
						message = String.format("The negative BizEvent message with id %s has already been processed previously, it is discarded",
								negativeBizEvtMsg.get(i).getId());
						logger.info(message);
					}
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

	public String findByBizEventId(String id) {
		return jedis.get(REDIS_ID_PREFIX+id);
	}

	public String saveBizEventId(String id) {
		return jedis.set(REDIS_ID_PREFIX+id, id, new SetParams().px(expireTimeInMS));
	}
}

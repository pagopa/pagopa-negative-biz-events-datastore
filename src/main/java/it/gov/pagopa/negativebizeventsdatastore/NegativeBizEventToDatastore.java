package it.gov.pagopa.negativebizeventsdatastore;

import com.google.common.base.Strings;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.OutputBinding;
import com.microsoft.azure.functions.annotation.BindingName;
import com.microsoft.azure.functions.annotation.Cardinality;
import com.microsoft.azure.functions.annotation.CosmosDBOutput;
import com.microsoft.azure.functions.annotation.EventHubTrigger;
import com.microsoft.azure.functions.annotation.ExponentialBackoffRetry;
import com.microsoft.azure.functions.annotation.FunctionName;

import it.gov.pagopa.negativebizeventsdatastore.client.RedisClient;
import it.gov.pagopa.negativebizeventsdatastore.entity.BizEvent;
import it.gov.pagopa.negativebizeventsdatastore.exception.AppException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.NonNull;
import redis.clients.jedis.Connection;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.params.SetParams;

/** Azure Functions with Azure Queue trigger. */
public class NegativeBizEventToDatastore {
	/** This function will be invoked when an Event Hub trigger occurs */

	public static final JedisPooled jedis = RedisClient.getInstance().redisConnectionFactory();

	private static final int EXPIRE_TIME_IN_MS = 
			System.getenv("REDIS_EXPIRE_TIME_MS") != null ? Integer.parseInt(System.getenv("REDIS_EXPIRE_TIME_MS")) : 3600000;

	private static final String REDIS_ID_PREFIX = "negbiz_";
	
	private static final int EBR_MAX_RETRY_COUNT = 5;

	@FunctionName("EventHubBizEventProcessor")
	@ExponentialBackoffRetry(maxRetryCount = EBR_MAX_RETRY_COUNT, maximumInterval = "00:15:00", minimumInterval = "00:00:10")
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
		
		int retryIndex = context.getRetryContext() == null ? 0 : context.getRetryContext().getRetrycount();
        if (retryIndex == EBR_MAX_RETRY_COUNT) {
        	logger.log(Level.WARNING, () -> String.format("[LAST RETRY] NegativeBizEventToDatastore function with invocationId [%s] performing the last retry for events ingestion", 
        			context.getInvocationId()));
		}
        
        logger.log(Level.INFO, () -> String.format("NegativeBizEventToDatastore function with invocationId [%s] called at [%s] with events list size [%s] and properties size [%s]", 
        		context.getInvocationId(), LocalDateTime.now(), negativeBizEvtMsg.size(), properties.length));

        StringJoiner eventDetails = new StringJoiner(", ", "{", "}");
		// persist the item
		try {
			if (negativeBizEvtMsg.size() == properties.length) {
				List<BizEvent> bizEvtMsgWithProperties = new ArrayList<>();

				for (int i = 0; i < negativeBizEvtMsg.size(); i++) {
					eventDetails.add("id: " + negativeBizEvtMsg.get(i).getId());
    	        	eventDetails.add("idPA: " + Optional.ofNullable(negativeBizEvtMsg.get(i).getCreditor()).map(o -> o.getIdPA()).orElse("N/A"));
    	        	eventDetails.add("modelType: " + Optional.ofNullable(negativeBizEvtMsg.get(i).getDebtorPosition()).map(o -> o.getModelType()).orElse("N/A"));
    	        	eventDetails.add("noticeNumber: " + Optional.ofNullable(negativeBizEvtMsg.get(i).getDebtorPosition()).map(o -> o.getNoticeNumber()).orElse("N/A"));
    	        	eventDetails.add("iuv: " + Optional.ofNullable(negativeBizEvtMsg.get(i).getDebtorPosition()).map(o -> o.getIuv()).orElse("N/A"));
    	        	
    	        	logger.log(Level.FINEST, () -> String.format("NegativeBizEventToDatastore function with invocationId [%s] working the biz-event [%s]",
							context.getInvocationId(), eventDetails));
    	        	
					// READ FROM THE CACHE: The cache is queried to find out if the event has already been queued --> if yes it is skipped
					String value = this.findByBizEventId(negativeBizEvtMsg.get(i).getId(), logger);
					if (Strings.isNullOrEmpty(value)) {
						BizEvent be = negativeBizEvtMsg.get(i);
						be.setProperties(properties[i]);
						// WRITE IN THE CACHE: The result of the insertion in the cache is logged to verify the correct functioning
						String result = this.saveBizEventId(negativeBizEvtMsg.get(i).getId(), logger);
						
						String msg = String.format("NegativeBizEventToDatastore function with invocationId [%s] cached biz-event message with id [%s] and result: [%s]",
								context.getInvocationId(), negativeBizEvtMsg.get(i).getId(), result);
						logger.finest(msg);

						bizEvtMsgWithProperties.add(be);
						
					} else {
						// just to track duplicate events  
						String msg = String.format("NegativeBizEventToDatastore function with invocationId [%s] has already processed and cached biz-event message with id [%s]: it is discarded",
								context.getInvocationId(), negativeBizEvtMsg.get(i).getId());
						logger.finest(msg);	
					}
				}
				
				documentdb.setValue(bizEvtMsgWithProperties);
				
			} else {
				throw new AppException("NegativeBizEventToDatastore function with invocationId [%s] - Error during processing - "
            			+ "The size of the events to be processed and their associated properties does not match [bizEvtMsg.size="
						+negativeBizEvtMsg.size()
						+"; properties.length="
						+properties.length
						+"]");
				
			}
		} catch (Exception e) {
			logger.severe("NegativeBizEventToDatastore function with invocationId [%s] "
            		+ "- Generic exception on cosmos biz-events msg ingestion at "
					+ LocalDateTime.now()
					+ " ["
					+eventDetails
					+"]: " 
					+ e.getMessage());
		}
	}

	public String findByBizEventId(String id, Logger logger) {
    	try (Connection j = jedis.getPool().getResource()){
    		return jedis.get(REDIS_ID_PREFIX+id);
    	} catch (Exception e) {
    		String msg = String.format("Error getting existing connection to Redis. A new one is created to GET the BizEvent message with id %s. [error message = %s]", 
    				REDIS_ID_PREFIX+id, e.getMessage());
    		logger.warning(msg);
    		// It try to acquire the connection again. If it fails, a null value is returned so that the data is not discarded
    		try (JedisPooled j = RedisClient.getInstance().redisConnectionFactory()){
    			return j.get(REDIS_ID_PREFIX+id);
    		} catch (Exception ex) {
    			return null;
    		}
    	}
    }
    
    public String saveBizEventId(String id, Logger logger) {
    	try (Connection j = jedis.getPool().getResource()){
    		return jedis.set(REDIS_ID_PREFIX+id, id, new SetParams().px(EXPIRE_TIME_IN_MS));
    	} catch (Exception e) {
    		String msg = String.format("Error getting existing connection to Redis. A new one is created to SET the BizEvent message with id %s. [error message = %s]", 
    				REDIS_ID_PREFIX+id, e.getMessage());
    		logger.warning(msg);
    		// It try to acquire the connection again. If it fails, a null value is returned so that the data is not discarded
    		try (JedisPooled j = RedisClient.getInstance().redisConnectionFactory()){
    			return j.set(REDIS_ID_PREFIX+id, id, new SetParams().px(EXPIRE_TIME_IN_MS));
    		} catch (Exception ex) {
    			return null;
    		}
    	}
    }
}

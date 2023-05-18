package it.gov.pagopa.negativebizeventsdatastore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.OutputBinding;
import it.gov.pagopa.negativebizeventsdatastore.entity.BizEvent;
import it.gov.pagopa.negativebizeventsdatastore.util.TestUtil;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NegativeBizEventsToEventHubTest {

  @Spy NegativeBizEventsToEventHub function;

  @Mock ExecutionContext context;

  @Test
  void runOk() throws IOException, IllegalArgumentException {

    Logger logger = Logger.getLogger("NegativeBizEventsToEventHub-test-logger");
    when(context.getLogger()).thenReturn(logger);

    List<BizEvent> items =
        TestUtil.readListModelFromFile("events/negativeBizEvents.json", BizEvent.class);
    assertEquals(13, items.size());

    @SuppressWarnings("unchecked")
    OutputBinding<List<BizEvent>> BizEventToAEH =
        (OutputBinding<List<BizEvent>>) mock(OutputBinding.class);
    @SuppressWarnings("unchecked")
    OutputBinding<List<BizEvent>> BizEventToFEH =
        (OutputBinding<List<BizEvent>>) mock(OutputBinding.class);

    // test execution
    function.processNegativeBizEventsToEventHub(items, BizEventToAEH, BizEventToFEH, context);
    ;

    // test assertion -> this line means the call was successful
    assertTrue(true);
  }
}

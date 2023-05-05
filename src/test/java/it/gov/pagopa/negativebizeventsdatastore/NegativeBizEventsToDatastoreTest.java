package it.gov.pagopa.negativebizeventsdatastore;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import it.gov.pagopa.negativebizeventsdatastore.entity.BizEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.OutputBinding;

@ExtendWith(MockitoExtension.class)
class NegativeBizEventToDataStoreTest {

    @Spy
    NegativeBizEventToDatastore function;

    @Mock
    ExecutionContext context;

    @Test
    void runOk() {
        // test precondition
        Logger logger = Logger.getLogger("NegativeBizEventToDataStore-test-logger");
        when(context.getLogger()).thenReturn(logger);

        List<BizEvent> bizEvtMsg = new ArrayList<>();
        bizEvtMsg.add (new BizEvent());

        Map<String, Object>[] properties = new HashMap[1];
        @SuppressWarnings("unchecked")
        OutputBinding<List<BizEvent>> document = (OutputBinding<List<BizEvent>>)mock(OutputBinding.class);

        // test execution
        function.processBizEvent(bizEvtMsg, properties, document, context);

        // test assertion -> this line means the call was successful
        assertTrue(true);
    }

    @Test
    void runKo_differentSize() {
        // test precondition
        Logger logger = Logger.getLogger("NegativeBizEventToDataStore-test-logger");
        when(context.getLogger()).thenReturn(logger);

        List<BizEvent> bizEvtMsg = new ArrayList<>();
        bizEvtMsg.add (new BizEvent());

        Map<String, Object>[] properties = new HashMap[0];
        @SuppressWarnings("unchecked")
        OutputBinding<List<BizEvent>> document = (OutputBinding<List<BizEvent>>)mock(OutputBinding.class);

        // test execution
        function.processBizEvent(bizEvtMsg, properties, document, context);

        // test assertion -> this line means the call was successful
        assertTrue(true);
    }
}

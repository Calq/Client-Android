package io.calq.android.analytics;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

public class QueuedApiCallTest {

    /**
     * Tests if the decoded payload matched the properties for this call.
     */
    @Test
    public void testPayloadMatchesInput() throws Exception {
        String payload = "{}";
        String endpoint = "fakeEndpoint";
        String writeKey = "dummykey_00000000000000000000000";

        long id = new Random().nextInt(100000);

        QueuedApiCall call = new QueuedApiCall(id, endpoint, payload, writeKey);

        assertEquals(id, call.getId());
        assertEquals(endpoint, call.getApiEndpoint());
        assertEquals(payload, call.getPayload());
        assertEquals(writeKey, call.getWriteKey());
    }
}
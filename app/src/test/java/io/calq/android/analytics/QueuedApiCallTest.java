package io.calq.android.analytics;

import org.junit.Test;

import java.util.Hashtable;
import java.util.Random;
import java.util.Map;

import org.json.JSONObject;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
public class QueuedApiCallTest {

    /**
     * Tests if the decoded payload matched the properties for this call.
     */
    @Test
    public void testPayloadMatchesInput() throws Exception {
        String payload = "{\"actor\":\"test\"}";
        String endpoint = "fakeEndpoint";
        String writeKey = "dummykey_00000000000000000000000";

        long id = new Random().nextInt(100000);

        QueuedApiCall call = new QueuedApiCall(id, endpoint, payload, writeKey);

        assertEquals(id, call.getId());
        assertEquals(endpoint, call.getApiEndpoint());
        assertEquals(writeKey, call.getWriteKey());

        // Payload wont exact match as it's modified - need to check
        JSONObject json = new JSONObject(call.getPayload());
        assertEquals("test", json.getString("actor"));
    }

    /**
     * Tests if the utc_now parameter was added and the timestamp is valid.
     */
    @Test
    public void testPayloadIncludesUtcTimestamp() throws Exception {
        Map<String, Object> properties = new Hashtable<String, Object>();
        String writeKey = "dummykey_00000000000000000000000";

        ActionApiCall actionCall = new ActionApiCall("TestActor", "Test Action", properties, writeKey);

        // Wrap real call into queued
        long id = new Random().nextInt(100000);
        QueuedApiCall call = new QueuedApiCall(id, actionCall.getApiEndpoint(),
                actionCall.getPayload(), actionCall.getWriteKey());

        // Now timestamp is injected when we call payload
        JSONObject json = new JSONObject(call.getPayload());
        assertNotNull(json.getString(ReservedApiProperties.UTC_NOW));
    }
}
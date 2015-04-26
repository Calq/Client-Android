package io.calq.android.analytics;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Hashtable;

import org.json.JSONObject;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class ActionApiCallTest {

    /**
     * The API call this is a test for.
     */
    private ActionApiCall call;

    /**
     * The actor for this test.
     */
    private String actor;

    /**
     * The test action;
     */
    private String action;

    /**
     * The test write key;
     */
    private String writeKey;

    /**
     * A test dictionary with custom properties.
     */
    Hashtable<String, Object> properties;

    @Before
    public void setUp() throws Exception {
        actor = "TestActor";
        action ="Test Action";
        writeKey = "dummykey_00000000000000000000000";

        properties = new Hashtable<String, Object>();
        properties.put("Test Property", "Test Value");

        call = new ActionApiCall(actor, action, properties, writeKey);
    }

    @After
    public void tearDown() throws Exception {

    }

    /**
     * Tests that API calls have an end point.
     */
    @Test
    public void testHasEndpoint() throws Exception {
        assertNotNull(call.getApiEndpoint());
    }

    /**
     * Tests that API calls have an payload
     */
    @Test
    public void testHasPayload() throws Exception {
        assertNotNull(call.getPayload());
    }

    /**
     * Tests if the decoded payload matched the properties for this call.
     */
    @Test
    public void testPayloadMatchesInput() throws Exception {
        JSONObject json = new JSONObject(call.getPayload());

        assertEquals(actor, json.getString(ReservedApiProperties.ACTOR));
        assertEquals(action, json.getString(ReservedApiProperties.ACTION_NAME));
        assertEquals(writeKey, json.getString(ReservedApiProperties.WRITE_KEY));

        JSONObject properties = json.getJSONObject(ReservedApiProperties.USER_PROPERTIES);

        assertNotNull(properties);
        assertEquals("Test Value", properties.getString("Test Property"));
    }

}
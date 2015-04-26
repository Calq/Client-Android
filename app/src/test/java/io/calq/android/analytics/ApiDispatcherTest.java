package io.calq.android.analytics;

import android.content.Context;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.RoboLayoutInflater;
import org.robolectric.tester.org.apache.http.TestHttpResponse;

import java.util.Hashtable;
import java.util.Vector;

import io.calq.android.ApiException;
import io.calq.android.LocalConfig;

import static org.junit.Assert.*;

@Config(emulateSdk = 18, manifest = "app/src/main/AndroidManifest.xml")
@RunWith(RobolectricTestRunner.class)
public class ApiDispatcherTest {

    /**
     * LocalConfig used to pass to ApiDispatcher instances (which has setup like API server URL)
     */
    private LocalConfig config;

    /**
     * A test batch of API calls.
     */
    private  Vector<QueuedApiCall> testBatch;


    @Before
    public void setUp() throws Exception {
        Context context = Robolectric.getShadowApplication().getApplicationContext();
        config = LocalConfig.getInstance(context);

        // Payload doesn't matter, we are testing response handling each time
        testBatch = new Vector<QueuedApiCall>();
        testBatch.add(new QueuedApiCall(0, "/Track", "{}", "dummykey_00000000000000000000000"));

        // Always want to intercept requests for our mocked response
        Robolectric.getFakeHttpLayer().interceptHttpRequests(true);
        Robolectric.getFakeHttpLayer().clearHttpResponseRules();
    }

    @After
    public void tearDown() throws Exception {

    }

    /**
     * Tests handling of a valid API call.
     */
    @Test
    public void testValidApiCall() throws Exception {
        ApiDispatcher dispatcher = new ApiDispatcher(config);

        Robolectric.getFakeHttpLayer().addHttpResponseRule(
                dispatcher.getEndpointUrl(testBatch.get(0)),
                new TestHttpResponse(200, "{\"status\":\"accepted\"}"));

        assertTrue(dispatcher.dispatch(testBatch));
    }

    /**
     * Tests handling of an invalid but formed API call (such as bad write key).
     */
    @Test(expected=ApiException.class)
    public void testApiException() throws Exception {
        ApiDispatcher dispatcher = new ApiDispatcher(config);

        Robolectric.getFakeHttpLayer().addHttpResponseRule(
                dispatcher.getEndpointUrl(testBatch.get(0)),
                new TestHttpResponse(400, "{\"status\":\"rejected\", \"error\":\"Test error\"}"));

        dispatcher.dispatch(testBatch); // Should throw
    }

    /**
     * Tests handling of an internal server error.
     */
    @Test
    public void testInternalServerError() throws Exception {
        ApiDispatcher dispatcher = new ApiDispatcher(config);

        Robolectric.getFakeHttpLayer().addHttpResponseRule(
                dispatcher.getEndpointUrl(testBatch.get(0)),
                new TestHttpResponse(500, "{\"status\":\"error\", \"error\":\"Test error\"}"));

        assertFalse(dispatcher.dispatch(testBatch));
    }
}
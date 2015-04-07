package io.calq.android;

import android.content.Context;

import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.math.BigDecimal;
import java.util.Hashtable;
import java.util.Random;

@Config(emulateSdk = 18, manifest = "app/src/main/AndroidManifest.xml")
@RunWith(RobolectricTestRunner.class)
public class CalqClientTest {

    /**
     * The write_key we use for making full test API calls (this is a test key designed for
     * these unit tests).
     */
    private String writeKey = "55ebeaebfcd351e0b69e6cc99dbb081d";

    /**
     * Random generator we use for building different IDs.
     */
    private Random rnd = new Random();

    /**
     * Context used to pass to CalqClient instances
     */
    private Context context;

    /**
     * LocalConfig used to pass to CalqClient instances (which has setup like API server URL)
     */
    private LocalConfig config;

    @Before
    public void setUp() throws Exception {
        ShadowApplication app = Robolectric.getShadowApplication();
        context = app.getApplicationContext();
        config = LocalConfig.getInstance(context);

        // Not mocked fully, so manually set permission
        app.grantPermissions("android.permission.INTERNET");
    }

    @After
    public void tearDown() throws Exception {

    }

    /**
     * Tests that calling our singleton with the same write key gives the same instance.
     */
    @Test
    public void testClientSharedInstance() throws Exception {
        CalqClient calq = CalqClient.getOrCreateClient(context, writeKey, config);
        CalqClient again = CalqClient.getOrCreateClient(context, writeKey, config);

        assertEquals(calq, again);
    }

    /**
     * Tests that identify is updating the actor property of the instance.
     */
    @Test
    public void testIdentifyUpdatesInstance() throws Exception {
        CalqClient calq = new CalqClient(context, CalqClient.generateAnonymousId(), writeKey, config);

        String identity = generateTestActor();
        calq.identify(identity);

        assertEquals(identity, calq.actor);
    }

    /**
     * Tests that calling identify twice doesn't update the 2nd time.
     */
    @Test
    public void testIdentifyFailsOnMultipleCalls() throws Exception {
        CalqClient calq = new CalqClient(context, CalqClient.generateAnonymousId(), writeKey, config);

        String identity = generateTestActor();
        calq.identify(identity);

        String again = generateTestActor();
        calq.identify(again);

        assertNotEquals(identity, again);
        assertEquals(identity, calq.actor);
    }

    /**
     * Tests that state is saved and loaded between different instances.
     */
    @Test
    public void testStatePersistence() throws Exception {
        CalqClient first = new CalqClient(context, CalqClient.generateAnonymousId(), writeKey, config);

        assertTrue(first.isAnon);

        String identity = generateTestActor();
        first.identify(identity);

        CalqClient second = new CalqClient(context, CalqClient.generateAnonymousId(), writeKey, config);
        second.loadState();

        assertFalse(first.isAnon);
        assertFalse(second.isAnon);
        assertNotEquals(first, second);
        assertEquals(first.actor, second.actor);
    }

    /**
     * Tests that calling clear sets a new anonymous user.
     */
    @Test
    public void testTrackUpdatesState() throws Exception {
        CalqClient calq = new CalqClient(context, CalqClient.generateAnonymousId(), writeKey, config);
        calq.track("Android Test Action (Anon)", null);
        assertTrue(calq.hasTracked);
    }

    /**
     * Tests that calling clear sets a new anonymous user.
     */
    @Test
    public void testClear() throws Exception {
        String anon = CalqClient.generateAnonymousId();
        CalqClient calq = new CalqClient(context, anon, writeKey, config);

        String identity = generateTestActor();
        calq.identify(identity);

        assertFalse(calq.isAnon);
        assertNotEquals(identity, anon);
        assertEquals(identity, calq.actor);

        calq.clear();

        assertTrue(calq.isAnon);
        assertFalse(calq.hasTracked);
        assertNotEquals(identity, calq.actor);
    }

    /**
     * Does a full test from raising an event and sending it to Calq. This test requires you give it a valid
     * Calq writeKey or it will not be able to send data.
     */
    @Test
    public void testEndToEndApiCalls() throws Exception {
        // For this test we actually want calls to go through. Don't mock HTTP
        Robolectric.getFakeHttpLayer().interceptHttpRequests(false);

        CalqClient calq = new CalqClient(context, CalqClient.generateAnonymousId(), writeKey, config);

        calq.track("Android Test Action (Anon)", null);

        calq.identify(generateTestActor());

        Hashtable<String, Object> actionProperties = new Hashtable<String, Object>();
        actionProperties.put("Test Property", "Test Value");
        calq.track("Android Test Action", actionProperties);

        calq.trackSale("Android Test Sale", null, "USD", new BigDecimal(100.00));

        Hashtable<String, Object> userProperties = new Hashtable<String, Object>();
        userProperties.put("$email", "test@notarealemail.com");
        userProperties.put("$full_name", calq.actor);
        calq.profile(userProperties);

        assertTrue(calq.flushQueue());
    }

    /**
     * Generates a test actor id.
     */
    private String generateTestActor() {
        return String.format("TestActor%d", rnd.nextInt(100000));
    }
}
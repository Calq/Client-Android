package io.calq.android.analytics;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Hashtable;
import java.util.Vector;

import static org.junit.Assert.*;

@Config(emulateSdk = 18, manifest = "app/src/main/AndroidManifest.xml")
@RunWith(RobolectricTestRunner.class)
public class ApiDataStoreTest {

    /**
     * Context used to pass to the store instance.
     */
    private Context context;

    /**
     * Dummy write key we use for indexing API calls in the store.
     */
    private String writeKey;

    @Before
    public void setUp() throws Exception {
        writeKey = "dummykey_00000000000000000000000";
        context = Robolectric.application.getApplicationContext();
    }

    @After
    public void tearDown() throws Exception {

    }

    /**
     * Tests that calling getWritableDatabase() multiple times returns a single instance.
     */
    @Test
    public void testGetWritableDatabaseReturnsSame() throws Exception {
        ApiDataStore store = new ApiDataStore(context);
        SQLiteDatabase db = store.getWritableDatabase();
        SQLiteDatabase again = store.getWritableDatabase();

        assertEquals(db, again);
    }

    /**
     * Tests that API calls can be added and removed from the queue (single).
     */
    @Test
    public void testAddAndRemoveSingle() throws Exception {
        ApiDataStore store = new ApiDataStore(context);

        // Should be empty to start
        assertNull(store.peekQueue(writeKey));

        // Add dummy call and check the queued call matched
        ActionApiCall call = createDummyActionCall();
        store.addToQueue(call);
        QueuedApiCall saved = store.peekQueue(writeKey);

        assertNotNull(saved);
        assertEquals(call.getApiEndpoint(), saved.getApiEndpoint());
        assertEquals(call.getPayload(), saved.getPayload());

        // Now remove and see if queue empty
        store.deleteFromQueue(saved);
        saved = store.peekQueue(writeKey);

        assertNull(saved);
    }

    /**
     * Tests that API calls can be added and removed from the queue (batch).
     */
    @Test
    public void testAddAndRemoveBatch() throws Exception {
        ApiDataStore store = new ApiDataStore(context);

        // Should be empty to start
        assertNull(store.peekQueue(writeKey));

        // Add two dummy calls and check batch size matched
        ActionApiCall call1 = createDummyActionCall();
        ActionApiCall call2 = createDummyActionCall();
        store.addToQueue(call1);
        store.addToQueue(call2);

        Vector<QueuedApiCall> batch = store.getBatch(writeKey, call1.getApiEndpoint());

        assertEquals(batch.size(), 2);
        assertEquals(batch.get(0).getApiEndpoint(), call1.getApiEndpoint());
        assertEquals(batch.get(0).getPayload(), call1.getPayload());
        assertEquals(batch.get(1).getPayload(), call2.getPayload());

        // Now remove and see if queue empty (as batch)
        store.deleteFromQueue(batch);
        Vector<QueuedApiCall> savedBatch = store.getBatch(writeKey, call1.getApiEndpoint());
        assertEquals(savedBatch.size(), 0);

        // And test as single
        QueuedApiCall saved = store.peekQueue(writeKey);

        assertNull(saved);
    }

    /**
     * Creates a dummy action call for use in our tests.
     */
    private ActionApiCall createDummyActionCall() {
        return new ActionApiCall("TestActor", "Test Action", new Hashtable<String, Object>(), writeKey);
    }
}
/*
 *  Copyright 2014 Calq.io
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in 
 *  compliance with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is 
 *  distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied. See the License for the specific language governing permissions and limitations under the 
 *  License.
 *  
 */

package io.calq.android.analytics;

import io.calq.android.ApiException;
import io.calq.android.LocalConfig;

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Vector;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/**
 * Class allows API calls to be handled in a non blocking manner.
 *
 */
public class ApiHandler extends Handler {
	
	private static final String TAG = "ApiHandler";
	
	/**
	 * The map between write keys and handler instances.
	 */
	private static Map<String, ApiHandler> handlerKayMap = new Hashtable<String, ApiHandler>(); 	// Yes, shared state. It's by design.
	
	/**
	 * The write key this handler is using.
	 */
	protected String writeKey;
	
	/**
	 * Queue that holds API calls that we have received but not yet persisted.
	 */
	protected Queue<AbstractAnalyticsApiCall> receiveQueue;
	
	/**
	 * Dispatcher used to actually issue API calls.
	 */
	protected ApiDispatcher dispatcher;
	
	/**
	 * The config we are using.
	 */
	protected LocalConfig config;
	
	/**
	 * Max delay between receiving a message and flushing it to storage if we haven't sent it yet.
	 */
	protected long flushStorageDelay = 3 * 1000;
		
	/**
	 * Data store used to persist API calls.
	 */
	protected ApiDataStore dataStore;

	
	private ApiHandler(Context context, String writeKey, LocalConfig config) {
		super(getNewThreadLooper());
		
		this.writeKey = writeKey;
		this.config = config;
		
		dataStore = new ApiDataStore(context);
		receiveQueue = new LinkedList<AbstractAnalyticsApiCall>();
		dispatcher = new ApiDispatcher(config);
		
		// Shecdule flush of any existing messages in 5s (Long enough so app can load / splash screen)
		scheduleFlushToRemote(5 * 1000);
	}
		
	/**
	 * Gets the handler that can be used for the given write key. Only one handler normally
	 * exists for each key.
	 * 
	 * @param context		The context to use for this handler.
	 * @param writeKey		The write key to get an ApiHandler for.
	 * @param config		The config to use.
	 * @return an ApiHandler for the given write key.
	 */
	public static ApiHandler getHandlerForKey(Context context, String writeKey, LocalConfig config) {
		synchronized (handlerKayMap) {
			ApiHandler handler = handlerKayMap.get(writeKey);
			if(handler == null) {
				handler = new ApiHandler(context, writeKey, config);
				handlerKayMap.put(writeKey, handler);
			}
			return handler;
		}
	}

	/**
	 * Handles when a new API call message has been received.
	 * 
	 * <p>When new messages arrive we want to return to the calling thread as soon
	 * as possible so it can get on with doing what it needs to. We store the call
	 * in a local queue and delay persisting for later.
	 * 
	 * @param apiCall		The API call that was received.
	 */
	public void apiCall(AbstractAnalyticsApiCall apiCall) {
		synchronized (receiveQueue) {
			receiveQueue.add(apiCall);
			scheduleFlushToStorage();	
			scheduleFlushToRemote();
		}
	}
	
	/**
	 * Handles when a it's time to flush outstanding messages to storage.
	 * 
	 * <p>Messages that are not pulled off the queue are persisted to storage. This
	 * way we don't lose them later. If a device has no network signal or other
	 * issue we can keep retrying to play them.
	 */
	protected void onFlushToStorage() {
		synchronized (receiveQueue) {
			while(!receiveQueue.isEmpty()) {
				dataStore.addToQueue(receiveQueue.remove());
			}
		}
	}
	
	/**
	 * Requests that we flush outstanding events immediately.
	 */
	public boolean forceFlush() {
        onFlushToStorage(); // In case we have any not yet saved, but we will want to flush them
        return onFlushToRemoteServer();
    }
	
	/**
	 * Handles when a it's time to flush queued messages to the API server.
	 * 
	 * <p>API calls which fail to send are not actually removed from the queue 
	 * (unless it was an API error from the server).
	 */
	protected boolean onFlushToRemoteServer() {
		synchronized (dataStore) {
			// Eat until we run out
			QueuedApiCall apiCall = null;
			while(null != (apiCall = dataStore.peekQueue(writeKey))) {
                try {
                    Vector<QueuedApiCall> batch = new Vector<QueuedApiCall>();
                    // Can we batch? Only track supports batching
                    if(apiCall.getApiEndpoint().equals(ActionApiCall.ENDPOINT)) {
                        batch = dataStore.getBatch(writeKey, apiCall.getApiEndpoint());
                    } else {
                        // Single call
                        batch.add(apiCall);
                    }

                    if(dispatcher.dispatch(batch)) {
						// Success. Delete this one
						dataStore.deleteFromQueue(batch);
					} else {
						// Failed. Probably network error. Retry later
						break;
					}
				} catch (ApiException e) {
					// API exceptions can't be replayed. They will fail again
					Log.e(TAG, "API exception returned from Calq: " + e.getMessage(), e);
					dataStore.deleteFromQueue(apiCall);
				}
			}
			
			// Empty now, don't fire again for a little
			scheduleFlushToRemote();

            return apiCall == null; // Whether we emptied the queue or not
		}
	}
	
	/**
	 * Handles API messages as they arrived.
	 * 
	 * @param msg			The message containing the API call to handle.
	 */
	@Override
	public void handleMessage(Message msg) {
		switch(msg.what) {

			case MESSAGE_FLUSH_TO_STORAGE:
				onFlushToStorage();
				break;
				
			case MESSAGE_FLUSH_TO_API_SERVER:
				onFlushToRemoteServer();
				break;
				
			default:
				throw(new IllegalArgumentException("Unknown message type of " + Integer.toString(msg.what)));
		}	
	}
	
	/**
	 * Request that we flush to storage after the next delay. Will be ignored if 
	 * a flush has already been scheduled.
	 */
	protected void scheduleFlushToStorage() {
		if(!hasMessages(MESSAGE_FLUSH_TO_STORAGE)) {
			Message msg = Message.obtain();
			msg.what = MESSAGE_FLUSH_TO_STORAGE;
			sendMessageDelayed(msg, flushStorageDelay);
		}
	}
	
	/**
	 * Request that we flush to Calq's API servers. Will be ignored if 
	 * a flush has already been scheduled.
	 */
	protected void scheduleFlushToRemote() {
		if(!hasMessages(MESSAGE_FLUSH_TO_API_SERVER)) {
			scheduleFlushToRemote(config.getRemoteFlushDelaySeconds() * 1000);
		}
	}
	
	/**
	 * Request that we flush to Calq's API servers. With the given delay.
	 * Does not check if a message is already pending.
	 * 
	 * @param delayMS		The delay before flushing (in MS).
	 */
	protected void scheduleFlushToRemote(int delayMS) {
		Message msg = Message.obtain();
		msg.what = MESSAGE_FLUSH_TO_API_SERVER;
		sendMessageDelayed(msg, delayMS);
	}
	
	/**
	 * Starts a new HandlerThread and gets a Looper for it so we can work off the main thread.
	 */
	private static Looper getNewThreadLooper() {
		HandlerThread thread = new HandlerThread(TAG);
		thread.start();
		return thread.getLooper();
	}

	/**
	 * Message indicating it's time to flush outstanding messages to storage.
	 */
	protected static final int MESSAGE_FLUSH_TO_STORAGE = 100;
	/**
	 * Message indicating it's time to flush outstanding messages to the remote API server.
	 */
	protected static final int MESSAGE_FLUSH_TO_API_SERVER = 101;

}

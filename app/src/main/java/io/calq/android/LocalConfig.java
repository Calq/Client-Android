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

package io.calq.android;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;

/**
 * Class handling loading Calq's configuration settings. Configuration settings 
 * are global (though certain properties can be changed directly on CalqClient 
 * instances) and as such there is a single instance.
 */
public class LocalConfig {
	
	private static final String TAG = "LocalConfig";

	/**
	 * Single instance. It's immutable so global state is OK.
	 */
	private static LocalConfig singleton;
	
	/**
	 * Lock used to create singleton.
	 */
	private static Object creationLock = new Object();
	
	/**
	 * Creates a new instance of a LocalConfig and reads settings. Typically
	 * you would use the {@link #getInstance(Context)} method to fetch the 
	 * singleton but this version is provided if you don't like global state.
	 * 
	 * @param context		The context used to fetch configuration items.
	 */
	public LocalConfig(Context context) {
        Bundle configBundles = new Bundle();
        readConfig(configBundles);
        return;

	/*	Context appContext = context.getApplicationContext();
		String packageName = appContext.getPackageName();
        try {
            ApplicationInfo appInfo = appContext.getPackageManager().getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            Bundle configBundle = appInfo.metaData;
            if (configBundle == null) {
                configBundle = new Bundle();
            }
            readConfig(configBundle);
        } catch (final NameNotFoundException e) {
            throw new RuntimeException("Unable to parse Calq configuration using package name = " + packageName, e);
        }*/
	}
	
	/**
	 * Gets (or creates) an instance of the LocalConfig.
	 * 
	 * @param context		The context used to fetch configuration items.
	 * @return a LocalConfig populated with current configuration.
	 */
	public static LocalConfig getInstance(Context context) {
		synchronized (creationLock) {
			if(singleton == null) {
				singleton = new LocalConfig(context);
			}
			return singleton;
		}
	}
	
	/**
	 * Reads the configuration settings from manifest.
	 * 
	 * @param configBundle	The bundle containing settings read from config.
	 */
	protected void readConfig(Bundle configBundle) {
		writeKey = configBundle.getString("io.calq.android.config.writeKey");
		
		remoteApiServerUrl = getStringWithDefault(configBundle, "io.calq.android.config.remoteApiServerUrl", remoteApiServerUrl);
		
		remoteFlushDelaySeconds = getRangeRestrictedInt(configBundle, "io.calq.android.config.remoteFlushDelaySeconds", remoteFlushDelaySeconds, 5, 120);
	}
	
	/**
	 * Attempts to fetch a value from config. If the value is there then it's value will be checked against
	 * the given range to see if it matches. Will only warn to log, will not throw an exception. If a value
	 * is outside the range the default value will be used.
	 */
	private int getRangeRestrictedInt(Bundle bundle, String key, int defaultValue, int min, int max) {
		int returnValue = defaultValue;
		int val = bundle.getInt(key);
		if(val != 0) {	// 0 indicates key miss
			if(val < min || val > max) {
				Log.w(TAG, "Invalid setting of " + val + " given for config key '" + key + 
						"'. Value should be between " + min + " and " + max);
			} else {
				returnValue = val;
			}
		}
		return returnValue;
	}
	
	/**
	 * Gets the given key from the bundle or uses the default value. This is required as pre
	 * we support API 8+ which doesn't have this method as part of the Bundle class (it was added
	 * at API 12)
	 */
	private String getStringWithDefault(Bundle bundle, String key, String defaultValue) {
		String val = bundle.getString(key);
		if(val == null || val.length() == 0) {
			return defaultValue;
		} else {
			return val;
		}
	}
	
	// The configuration settings along with defaults
	
	/**
	 * Gets the write key used to send API messages.
	 * 
	 * <p>Set by key: io.calq.android.config.writeKey
	 */
	public String getWriteKey() { return writeKey; }
	protected String writeKey = null;	// No default!
	
	/**
	 * Gets the time between flushing API calls to the remote server (in seconds).
	 * Should be between 5 and 120s.
	 * 
	 * <p>Set by key: io.calq.android.config.remoteFlushDelaySeconds
	 */
	public int getRemoteFlushDelaySeconds() { return remoteFlushDelaySeconds; }
	protected int remoteFlushDelaySeconds = 45;

	/**
	 * Gets the remote API server to use.
	 * 
	 * <p>Set by key: io.calq.android.config.remoteApiServerUrl
	 */
	public String getRemoteApiServerUrl() { return remoteApiServerUrl; }
	protected String remoteApiServerUrl = "https://api.calq.io/";
	
	
	
}

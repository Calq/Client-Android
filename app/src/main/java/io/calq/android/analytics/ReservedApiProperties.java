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

public class ReservedApiProperties {

	/**
     * The name of a new action.
     */
    public static final String ACTION_NAME = "action_name";

    /**
     * The unique actor of an event (e.g. a user id, server name, etc).
     */
    public static final String ACTOR = "actor";

    /**
     * The source ip address for this action.
     */
    public static final String IP_ADDRESS = "ip_address";

    /**
     * The previous unique id of an actor when transferring.
     */
    public static final String OLD_ACTOR = "old_actor";

    /**
     * The new unique id of an actor when transferring.
     */
    public static final String NEW_ACTOR = "new_actor";

    /**
     * Properties node giving user provided custom information.
     */
    public static final String USER_PROPERTIES = "properties";

    /**
     * The timestamp of a new event.
     */
    public static final String TIMESTAMP = "timestamp";

    /**
     * The unique key to identify this project when writing.
     */
    public static final String WRITE_KEY = "write_key";
    
}

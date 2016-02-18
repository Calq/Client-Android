Calq Android Client
=================

The full quick start and reference docs can be found at: https://www.calq.io/docs/client/android

Installation
------------

For Android Studio simply add Calq as a gradle dependency in `build.gradle`

```
dependencies {
	/* ... */ 
    compile 'io.calq:library:1.1.+'
}
```

Detailed instructions for Android Studio and alternative instructions for Eclipse are available in the [Quick Start Guide](https://www.calq.io/docs/client/android).

Your app must have the `INTERNET` permission. This is needed so the Calq client can send analytics data back to our servers.

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

You also need to add your Calq write key to the manifest within your `application` node. You can find your key inside the Calq reporting interface. Remember to replace the text 'YOUR_WRITE_KEY_HERE' with your real key.

```xml
<application ...>

    <!-- ... normally <activity ...> and other config here ... -->

    <meta-data
        android:name="io.calq.android.config.writeKey"
        android:value="YOUR_WRITE_KEY_HERE" />
    
</application>
```

Getting a client instance
-------------------------

The easiest way to get an instance of the client is to use the static CalqClient.getOrCreateClient method. This will create a client and load any existing user information or properties that have been previously set. If this is the first time the client has been used then new data is generated.

```java
// Get an instance using previous data if possible
CalqClient calq = CalqClient.getOrCreateClient(context);
```


Tracking actions
----------------

Calq performs analytics based on actions that user's take. You can track an action using `CalqClient.track`. Specify the action and any associated data you wish to record.

```java
// Track a new action called "Product Review" with a custom rating
CalqClient calq = CalqClient.getOrCreateClient(context);

Map<String, Object> extras = new Hashtable<String, Object>();
extras.put("Rating", 9.0);

calq.track("Product Review", extras);
```

The dictionary parameter allows you to send additional custom data about the action. This extra data can be used to make advanced queries within Calq.

Documentation
-------------

The full quick start can be found at: https://www.calq.io/docs/client/android

The reference can be found at:  https://www.calq.io/docs/client/android/reference

License
--------

[Licensed under the Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).






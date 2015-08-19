# Phonegap android uDynamo plugin

Simple plugin to use uDynamo in android apps made with phonegap.

## Installing

Install the plugin

    $ phonegap plugin add https://github.com/CODESM003/uDynamo-Phonegap-Android.git

Add this to AndroidManifest.xml

    <uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />


## Using the plugin

`isDeviceConnected` is used to check if the reader is connected.
It actually checks if anything is plugged in the headphone jack but should be okay for a POS type system.

```js
    var success = function() {
        alert("Device is connected");
    }

    var failure = function() {
        alert("Device is not connected");
    }

    udynamo.isDeviceConnected(success, failure);
```

`getSwipeData` opens the device and waits for a card to be swiped then returns the data as json.

```js
    var success = function(data) {
        alert("Your card track 1 data is " + data.Track1);
    }

    var failure = function(response) {
        if(response === 'Error') {
            alert("device is not connected"); // better to check for this with 'isDeviceConnected'
            return;
        }
        
        alert(response); // The card was not property swiped. Please try again.
    }

    udynamo.getSwipeData(success, failure);
```

`cancelSwipe` cancels the `getSwipeData` request and closes the device.

```js
    var success = function() {
        alert("Device closed successfully");
    }

    var failure = function() {
        alert("Failed to close the device. Try replugging it.");
    }

    udynamo.cancelSwipe(success, failure);
```

## Credits

[Magtek Official sdk and docs](http://www.magtek.com/support/software/programming_tools/)

[eGood/CardReaderPlugin](https://github.com/eGood/CardReaderPlugin)

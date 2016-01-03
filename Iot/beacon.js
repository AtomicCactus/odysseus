var bleno = require('bleno');

//iBeacon info
var uuid  = 'e2c56db5dffb48d2b060d0f5a71096e0';
var major = 0;
var minor = 0;
var measuredPower = -59;

bleno.on('stateChange', function(state) {
    console.log('on -> stateChange: ' + state);

    if (state === 'poweredOn') {
        bleno.startAdvertisingIBeacon(
            uuid, major, minor, measuredPower);
    }
    else if (state === 'unsupported') {
        console.log("Error, state is unsupported");
    }
    else {
        bleno.stopAdvertising();
    }
});

bleno.on('advertisingStart', function() {
    console.log('Advertising Started');
});

bleno.on('advertisingStop', function() {
    console.log('Advertising stopped');
});
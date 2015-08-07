/*global cordova, module*/

module.exports = {
    isDeviceConnected: function(successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "uDynamo", "isDeviceConnected", []);
    },
    getSwipeData: function(successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "uDynamo", "getSwipeData", []);
    },
    cancelSwipe: function(successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "uDynamo", "cancelSwipe", []);
    }
};

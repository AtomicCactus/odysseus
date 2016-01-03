'use strict';

/**
 * @ngdoc function
 * @name odysseusApp.controller: AppsController
 * @description
 * # AppsController
 * Apps list Controller of the odysseusApp
 */
angular.module('odysseusApp')
  .controller('AppsController', function ($scope, $firebaseArray, Ref) {

    $scope.apps = $firebaseArray(Ref);

    // display any errors
    $scope.apps.$loaded().catch(alert);

    $scope.onAppClicked = function (app) {
      Ref.child(app.$id + '/enabled').set(app.enabled);
    }

  });

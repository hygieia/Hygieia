(function () {
    'use strict';

    angular
        .module(HygieiaConfig.module)
        .controller('scoreBoardDetailsController', scoreBoardDetailsController);

    scoreBoardDetailsController.$inject = ['$scope', '$uibModalInstance', 'scoreBoardDetailsConfig'];
    function scoreBoardDetailsController($scope, $uibModalInstance, scoreBoardDetailsConfig) {
        /*jshint validthis:true */
        var ctrl = this;

        $scope.metricName = scoreBoardDetailsConfig.metricName;
        $scope.teamName = scoreBoardDetailsConfig.teamName;
        $scope.score = scoreBoardDetailsConfig.metricScore;
        $scope.value = scoreBoardDetailsConfig.metricValue;

        scoreBoardDetailsConfig.scoreBoardMetrics.forEach(function(metricData) {
           if(metricData.metricName == $scope.metricName) {
               $scope.displayName = metricData.displayName;
           }
        });

    }
})();

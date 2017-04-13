(function () {
    'use strict';

    angular
        .module(HygieiaConfig.module + '.core')
        .factory('userData', userData);

    function userData($http) {
        var testDetailRoute = 'test-data/signup_detail.json';
        var adminRoute = '/api/admin';

        return {
            getAllUsers: getAllUsers,
            promoteUserToAdmin: promoteUserToAdmin,
            demoteUserFromAdmin: demoteUserFromAdmin
        };


        // reusable helper
        function getPromise(route) {
            return $http.get(route).then(function (response) {
              console.log("Data="+ JSON.stringify(response.data));
                return response.data;
            });
        }

      function getAllUsers(){

          if(HygieiaConfig.local)
          {
            console.log("In local testing");
            return getPromise(testDetailRoute);
          }
          else
          {
        return $http.get(adminRoute + "/users");
      }
    }

    function promoteUserToAdmin(user) {
        var route = adminRoute + "/users/" + user.authType + "/" + user.username + "/authorities";
        var postData = "\"ROLE_ADMIN\"";
        return $http.post(route, postData);
    }

    function demoteUserFromAdmin(user) {
      var route = adminRoute + "/users/" + user.authType + "/" + user.username + "/authorities/ROLE_ADMIN";
      return $http.delete(route);
    }

  }
})();

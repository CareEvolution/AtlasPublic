define((require) => {
	const { Route } = require('pages/Route');
	const authApi = require('services/AuthAPI');
	const sharedState = require('atlas-state');
	const stateConstants = require('const-state');
	let appStatusSubscription = null;

	function routes(router) {
		return {
			'/': new Route(() => {
				document.location = '#/datasources';
			}),
			'/welcome/:authClient/reloginRequired': new Route((authClient) => {
				require([], function () {
					setAuth(authClient, true, '/datasources');
				});
			}),
			'/welcome/:authClient:/?((w|.)*)': new Route((authClient, path) => {
				require([], function () {
					setAuth(authClient, false, decodeURIComponent(path));
				});
			}),
		};
	}

	function setAuth(authClient, reloginRequired, url) {
		authApi.reloginRequired(reloginRequired);
		authApi.authClient(authClient);
		if (!reloginRequired) {
			// We load user info, which subsequently triggers the data sources to be loaded. The application
			// status changes to running after the data sources are loaded.
			// At that point we set the route. This way, if the route is the datasources page, the 
			// data sources will be loaded and ready for use, and the correct report will be loaded.
			appStatusSubscription =
				sharedState.appInitializationStatus.subscribe((status) => {
					if (status === stateConstants.applicationStatuses.running) {
						document.location = '#' + url;
						appStatusSubscription.dispose();
					}
				});
			authApi.loadUserInfo();
		} else {
			authApi.signInOpened(true);
			document.location = '#' + url;
		}
	}

	return routes;
});

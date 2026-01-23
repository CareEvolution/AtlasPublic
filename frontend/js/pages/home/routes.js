define((require) => {
	const { Route } = require('pages/Route');

	function routes(router) {
		return {
			'/home': new Route(() => {
				require(['./home'], function () {
					router.setCurrentView('home');
				});
			}),
		};
	}

	return routes;
});

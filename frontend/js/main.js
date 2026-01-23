require(['./settings'], (settings) => {
	requirejs.config({
		...settings,
		paths: {
			...settings.paths,
			...settings.localRefs,
		},
	});
	require([
		'jquery',
		'bootstrap',
		'ko.sortable',
		'databindings',
		'services/PluginRegistry',
	], function () {
		// bootstrap must come first
		require([
			'knockout',
			'Application',
			'appConfig',
			'const',
			'pages/Router',
			'atlas-state',
			'loading',
			'user-bar',
			'welcome',
			'components/white-page',
			'components/terms-and-conditions/terms-and-conditions',
		], (ko, Application, appConfig, constants, router, sharedState) => {
			const app = new Application(router);

			// This was added to avoid multiple UI updates,
			// but this can break code that depends on synchronous updates
			// More details about deferred updates available at https://knockoutjs.com/documentation/deferred-updates.html
			ko.options.deferUpdates = true;

			app.bootstrap()
				.then(() => app.checkOAuthError())
				.then(() => app.synchronize())
				.then(() =>
					require(appConfig.externalLibraries, () =>
						console.log('Loaded external plugins'))
				)
				.catch((er) => {
					sharedState.appInitializationStatus(
						constants.applicationStatuses.failed
					);
					console.error('App initialization failed', er);
				});

			return app;
		});
	});
});

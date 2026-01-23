define((require) => {
	const { AuthorizedRoute } = require('pages/Route');
	function routes(router) {
		return {
			'/datasources': new AuthorizedRoute(() => {
				require(['./data-sources'], function () {
					router.setCurrentView('data-sources', {
						reportName: undefined,
						sourceKey: undefined,
					});
				});
			}),
			'/datasources/:sourceKey': new AuthorizedRoute(
				(sourceKey) => {
					require(['./data-sources'], function () {
						router.setCurrentView('data-sources', {
							reportName: undefined,
							sourceKey: sourceKey
								? decodeURIComponent(sourceKey)
								: sourceKey,
						});
					});
				}
			),
			'/datasources/:sourceKey/:reportName': new AuthorizedRoute(
				(sourceKey, reportName) => {
					require(['./data-sources'], function () {
						router.setCurrentView('data-sources', {
							reportName: reportName
								? decodeURIComponent(reportName)
								: reportName,
							sourceKey: sourceKey
								? decodeURIComponent(sourceKey)
								: sourceKey,
						});
					});
				}
			),
		};
	}

	return routes;
});

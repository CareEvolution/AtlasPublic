define((require) => {
	const { AuthorizedRoute } = require('pages/Route');
	const sharedState = require('atlas-state');
	function routes(router) {
		return {
			'/cohortdefinitions': new AuthorizedRoute(() => {
				require([
					'./cohort-definitions',
				], function () {
					router.setCurrentView('cohort-definitions');
				});
			}),

			'/cohortdefinition/:cohortDefinitionId/samples':
				new AuthorizedRoute((cohortDefinitionId) => {
					require([
						'./cohort-definition-manager',
					], function () {
						// not re-render component if it was rendered already
						router.setCurrentView('cohort-definition-manager', {
							cohortDefinitionId,
							mode: 'samples',
						});
						sharedState.CohortDefinition.mode('samples');
					});
				}),

			'/cohortdefinition/:cohortDefinitionId/samples/:sourceKey':
				new AuthorizedRoute((cohortDefinitionId, sourceKey) => {
					require([
						'./cohort-definition-manager',
					], function () {
						router.setCurrentView('cohort-definition-manager', {
							cohortDefinitionId,
							sourceKey,
							mode: 'samples',
						});
						sharedState.CohortDefinition.mode('samples');
					});
				}),

			'/cohortdefinition/:cohortDefinitionId/samples/:sourceKey/:sampleId':
				new AuthorizedRoute(
					(cohortDefinitionId, sourceKey, sampleId) => {
						require([
							'./cohort-definition-manager',
						], function () {
							router.setCurrentView('cohort-definition-manager', {
								cohortDefinitionId,
								sampleId,
								sourceKey,
								mode: 'samples',
							});
							sharedState.CohortDefinition.mode('samples');
						});
					}
				),

			'/cohortdefinition/:cohortDefinitionId/conceptsets/:conceptSetId/:mode':
				new AuthorizedRoute(
					(cohortDefinitionId, conceptSetId, mode) => {
						require([
							'components/conceptset/ConceptSetStore',
							'./cohort-definition-manager',
						], function (ConceptSetStore) {
							sharedState.CohortDefinition.mode('conceptsets');
							sharedState.activeConceptSet(
								ConceptSetStore.cohortDefinition()
							);
							router.setCurrentView('cohort-definition-manager', {
								cohortDefinitionId,
								mode: 'conceptsets',
								conceptSetId,
							});
						});
					}
				),

			'/cohortdefinition/:cohortDefinitionId/version/:version':
				new AuthorizedRoute((cohortDefinitionId, version) => {
					require([
						'./cohort-definition-manager',
					], function () {
						router.setCurrentView('cohort-definition-manager', {
							cohortDefinitionId,
							version,
							mode: 'definition',
						});
						sharedState.CohortDefinition.mode('definition');
					});
				}),

			'/cohortdefinition/:cohortDefinitionId:/?((w|.)*)':
				new AuthorizedRoute(
					(cohortDefinitionId, path = 'definition') => {
						require([
							'./cohort-definition-manager',
						], function () {
							// Determine the view to show on the cohort manager screen based on the path
							path = path.split('/');
							let view = 'definition';
							if (path.length > 0 && path[0] != '') {
								view = path[0];
							}
							let selectedSourceId = null;
							if (path.length > 1 && path[1] != '') {
								selectedSourceId = parseInt(path[1]);
							}
							// Determine any optional parameters to set based on the query string
							const qs = router.qs(); // Get the query string parameters
							const sourceKey = qs.sourceKey || null;
							router.setCurrentView('cohort-definition-manager', {
								cohortDefinitionId,
								selectedSourceId,
								mode: view,
								sourceKey,
							});
							sharedState.CohortDefinition.mode(view);
						});
					}
				),
		};
	}

	return routes;
});

define([
	'knockout',
	'text!./cohort-definitions.html',
	'appConfig',
	'services/AuthAPI',
	'pages/Page',
	'utils/CommonUtils',
	'atlas-state',
	'pages/cohort-definitions/const',
	'databindings',
	'less!./cohort-definitions.less',
	'components/heading',
	'components/entityBrowsers/cohort-definition-browser',
	'components/ac-access-denied',
	'./components/cohort-definition-importer',
], function (
	ko,
	view,
	config,
	authApi,
	Page,
	commonUtils,
	sharedState,
	constants
) {
	class CohortDefinitions extends Page {
		constructor(params) {
			super(params);
			this.cohortDefinitionId = ko.observable();
			this.cohortDefinitionId.extend({
				notify: 'always',
			});
			this.currentCohortDefinition = sharedState.CohortDefinition.current;
			this.showImportDialog = ko.observable(false);

			this.cohortDefinitionId.subscribe((id) => {
				commonUtils.routeTo(constants.paths.details(id));
			});

			this.isAuthenticated = authApi.isAuthenticated;
			this.canReadCohorts = ko.pureComputed(
				() =>
					(config.userAuthenticationEnabled &&
						this.isAuthenticated()) ||
					!config.userAuthenticationEnabled
			);

			this.canCreateCohort = ko.observable(false);

			authApi.canCreateCohortDefinition().then((result) => {
				this.canCreateCohort(result);
			});

			this.tableOptions = commonUtils.getTableOptions('L');
		}

		newDefinition() {
			this.cohortDefinitionId('0');
		}

		import() {
			this.showImportDialog(true);
		}
	}

	commonUtils.build('cohort-definitions', CohortDefinitions, view);
});

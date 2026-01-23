define([
	'knockout',
	'text!./home.html',
	'pages/Page',
	'utils/CommonUtils',
	'services/AuthAPI',
	'atlas-state',
	'version',
	'components/heading',
], function (
	ko,
	view,
	Page,
	commonUtils,
	authApi,
	sharedState,
	version
) {
	class Home extends Page {
		constructor(params) {
			super(params);
			this.github_status = ko.observableArray();

			this.canCreateCohort = ko.observable(false);

			authApi.canCreateCohortDefinition().then((result) => {
				this.canCreateCohort(result);
			});

			this.currentCohortDefinition = sharedState.CohortDefinition.current;
			this.canSearch = ko.computed(() => {
				return authApi.isAuthenticated();
			});
			this.atlasVersion = ko.observable(this.formatVersion(version));
			this.currentYear = ko.observable(new Date().getFullYear());
			this.loading = ko.observable();
		}

		formatVersion(ver) {
			return ver.replace(/(\d+\.\d+\.\d+)-(.*)/, '$1 $2');
		}

		newCohortDefinition() {
			document.location = '#/cohortdefinition/0';
		}

		browseVocabulary() {
			document.location = '#/search';
		}
	}

	return commonUtils.build('home', Home, view);
});

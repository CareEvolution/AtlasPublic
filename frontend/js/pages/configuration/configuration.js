define([
	'knockout',
	'text!./configuration.html',
	'pages/Page',
	'utils/AutoBind',
	'utils/CommonUtils',
	'appConfig',
	'services/AuthAPI',
	'less!./configuration.less',
	'./sources/source-list',
], function (
	ko,
	view,
	Page,
	AutoBind,
	commonUtils,
	config,
	authApi,
) {
	class Configuration extends AutoBind(Page) {
		constructor(params) {
			super(params);
			this.params = params;
			this.activeTab = ko.observable('sources');
			this.activeComponent = ko.observable('source-list');
			this.isAuthenticated = authApi.isAuthenticated;
			this.hasPageAccess = ko.pureComputed(() => {
				return (
					(config.userAuthenticationEnabled &&
						this.isAuthenticated()) ||
					!config.userAuthenticationEnabled
				);
			});
			this.canReadRoles = ko.observable(false);
			authApi.canReadRoles().then((result) => {
				this.canReadRoles(result);
			});

			this.permissionsAccess = ko.pureComputed(() => {
				return this.canReadRoles();
			});
		}

		onRouterParamsChanged(params, newParams) {
			let { section, component } = Object.assign({}, params, newParams);

			if (!section) {
				section = 'sources';
			}

			if (!component) {
				component = 'source-list';
			}

			this.activeTab(section);
			this.activeComponent(component);
		}
	}

	return commonUtils.build('ohdsi-configuration', Configuration, view);
});

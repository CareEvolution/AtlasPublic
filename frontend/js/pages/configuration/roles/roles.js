define([
	'knockout',
	'text!./roles.html',
	'pages/Page',
	'utils/AutoBind',
	'utils/CommonUtils',
	'services/AuthAPI',
	'services/role',
	'services/DatatableLayoutService',
	'databindings',
	'components/ac-access-denied',
	'components/heading',
], function (
	ko,
	view,
	Page,
	AutoBind,
	commonUtils,
	authApi,
	roleService,
	layoutService
) {
	class Roles extends AutoBind(Page) {
		constructor(params) {
			super(params);
			this.roles = ko.observable([]);
			this.loading = ko.observable();
			this.tableOptions = commonUtils.getTableOptions('L');
			this.isAuthenticated = authApi.isAuthenticated;
			this.canRead = ko.observable(true);
			this.canCreateRole = ko.observable(false);
			authApi.canCreateRole().then((result) => {
				this.canCreateRole(result);
			});
			this.canCreate = ko.pureComputed(() => {
				return (
					this.isAuthenticated() && this.canCreateRole()
				);
			});
			this.layout = layoutService.basicLayoutWithoutButtons;
		}

		onPageCreated() {
			this.loading(true);
			roleService.updateRoles().then((roles) => {
				this.roles(roles);
				this.loading(false);
			});
		}

		selectRole(data) {
			commonUtils.routeTo('/role/' + data.id);
		}

		newRole() {
			commonUtils.routeTo('/role/0');
		}

		importRoles() {
			commonUtils.routeTo('/import/roles');
		}
	}

	return commonUtils.build('roles', Roles, view);
});

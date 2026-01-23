define([
	'knockout',
	'text!./users.html',
	'utils/AutoBind',
	'components/Component',
	'utils/CommonUtils',
	'services/AuthAPI',
	'services/DatatableLayoutService',
	'databindings',
], function (
	ko,
	view,
	AutoBind,
	Component,
	commonUtils,
	authApi,
	layoutService
) {
	class UserView extends AutoBind(Component) {
		constructor(params) {
			super(params);

			this.isNewRole = params.isNewRole;
			this.roleId = params.roleId;
			this.userItems = params.userItems;
			this.tableOptions = commonUtils.getTableOptions('L');
			this.areUsersSelected = ko.pureComputed(() => {
				return !!this.userItems().find((user) => user.isRoleUser());
			});
			this.canEditRole = params.canEditRole;
			this.layout = layoutService.basicLayoutWithoutButtons;
		}

		selectAllUsers() {
			this.userItems().forEach((user) => user.isRoleUser(true));
		}

		deselectAllUsers() {
			this.userItems().forEach((user) => user.isRoleUser(false));
		}

		renderCheckbox(field, editable) {
			return editable
				? '<span data-bind="click: function(d) { d.' +
						field +
						'(!d.' +
						field +
						'()); } , css: { selected: ' +
						field +
						'}" class="fa fa-check custom-check-box"></span>'
				: '<span data-bind="css: { selected: ' +
						field +
						'}" class="fa fa-check custom-check-box readonly"></span>';
		}
	}

	return commonUtils.build('users', UserView, view);
});

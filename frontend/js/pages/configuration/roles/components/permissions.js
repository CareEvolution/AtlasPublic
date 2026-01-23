define([
	'knockout',
	'text!./permissions.html',
	'components/Component',
	'utils/AutoBind',
	'utils/CommonUtils',
	'services/AuthAPI',
	'services/DatatableLayoutService',
	'databindings',
], function (
	ko,
	view,
	Component,
	AutoBind,
	commonUtils,
	authApi,
	layoutService
) {
	class PermissionsView extends AutoBind(Component) {
		constructor(params) {
			super(params);

			this.isNewRole = params.isNewRole;
			this.roleId = params.roleId;
			this.permissionItems = params.permissionItems;
			this.tableOptions = commonUtils.getTableOptions('L');
			this.canEditRole = params.canEditRole;
			this.layout = layoutService.basicLayoutWithoutButtons;
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

	return commonUtils.build('permissions', PermissionsView, view);
});

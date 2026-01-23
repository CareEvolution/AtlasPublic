define([
	'knockout',
	'text!./atlas-roles.html',
	'components/Component',
	'utils/CommonUtils',
	'utils/Renderers',
	'services/DatatableLayoutService',
],
	function(
		ko,
		view,
		Component,
		commonUtils,
		renderers,
		layoutService,
	){

		class AtlasRoles extends Component {
			constructor(params) {
				super(params);
				this.roles = params.roles || [];
				this.tableOptions = commonUtils.getTableOptions('L');
				this.renderCheckbox = this.renderCheckbox.bind(this);
				this.layout = layoutService.basicLayout;
			}

			renderCheckbox(field) {
				return renderers.renderCheckbox(field);
			}
		}

		commonUtils.build('atlas-roles', AtlasRoles, view);
	}
);
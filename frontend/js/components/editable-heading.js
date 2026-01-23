define([
	'knockout',
	'text!./editable-heading.html',
	'components/Component',
	'utils/CommonUtils',
	'../const',
	'components/tags/badges-list/tags-badges-list',
	'less!./editable-heading.less'
], function (
	ko,
	view,
	Component,
	commonUtils,
	constants
) {
	class EditableHeading extends Component {
		constructor(params) {
			super(params);
			this.name = params.name;
			this.canEdit = params.canEdit;
			this.fieldId = params.fieldId;
			this.meta = params.meta;
			this.authorship = params.authorship;
			this.description = params.description;
			this.icon = params.icon || null;
			this.hasIcon = ko.computed(() => {
				return this.icon != null;
			});
			this.description = params.description;
			this.tags = params.tags;

			const newEntityNames = Object.values(constants.newEntityNames).map( n => n() ).concat(
				['New Role', 'New Source']
			);

			this.editing = ko.observable(newEntityNames.indexOf(this.name()) === -1 ? false : true);
		}

		startEdit() {
			this.editing(true);
		}

		commitEdit() {
			this.editing(false);
		}

		nonEmptyName() {
			return this.name() && this.name().trim();
		}
	}

	return commonUtils.build('editable-heading', EditableHeading, view);
});
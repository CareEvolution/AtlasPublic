define([
	'knockout',
	'text!./tabbed-grid.html',
	'components/Component',
	'utils/CommonUtils',
	'pages/characterizations/const',
	'databindings',
	'less!./tabbed-grid.less',
	'components/heading',
	'faceted-datatable',
], function (
	ko,
	view,
	Component,
	commonUtils,
	constants
) {
	class CharacterizationsTabbedGrid extends Component {
		constructor(params) {
			super();

			this.tabs = constants.gridTabs;
			this.activeTab = params.activeTab;

			this.isViewPermitted = params.isViewPermitted;
			this.data = params.data;
			this.gridColumns = params.gridColumns;
			this.gridOptions = params.gridOptions;
			this.order = params.order || [[3, 'desc']];

			this.createNew = params.createNew;
			this.createNewEnabled =
				typeof params.createNewEnabled === 'undefined'
					? () => true
					: params.createNewEnabled;
			this.createNewLabel = constants.gridTabs.filter(
				(t) => t.value === params.activeTab
			).newEntityLabel;
			this.tableOptions = commonUtils.getTableOptions('L');
		}

		get datatableLanguage() {
			return ko.i18n('datatable.language');
		}
	}

	return commonUtils.build(
		'characterizations-tabbed-grid',
		CharacterizationsTabbedGrid,
		view
	);
});

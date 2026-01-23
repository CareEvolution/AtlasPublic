define([
	'knockout', 
	'jquery',
	'services/DatatableLayoutService',
	'utils/CommonUtils', 
	'text!./conceptset-table.html', 
	'databindings'], 
	function (ko, $, layoutService, commonUtils, template) {

	function ConceptSetTable(params) {
		const self = this;
		self.title = params.title || 'Concept List';
		self.conceptSetList = params.conceptSetList;
		self.isShown = params.isShown;
		self.commonUtils = commonUtils;
		self.tableOptions = commonUtils.getTableOptions('M');
		self.layout = layoutService.basicLayout;
	}
	const component = {
		viewModel: ConceptSetTable,
		template: template
	};

	ko.components.register('conceptset-table', component);

	return component;
});
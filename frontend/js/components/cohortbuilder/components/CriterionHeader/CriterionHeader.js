define([
	'knockout',
	'text!./CriterionHeader.html',
], function (
	ko,
	template,
) {
	function CriterionHeaderViewModel(params) {
		var self = this;
		self.criterionTitle = params.criterionTitle || '';
		self.conceptSetInfo = params.conceptSetInfo;
		self.hideClose = params.hideClose;
		self.addActions = params.addActions;
		self.removeCriteria = params.removeCriteria;
		self.criteria = params.criteria;
	}

	// return component definition
	return {
		viewModel: CriterionHeaderViewModel,
		template: template,
	};
});

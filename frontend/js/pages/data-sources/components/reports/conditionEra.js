define([
	'text!./treemap.html',
	'components/reports/classes/Treemap',
	'components/reports/const',
	'utils/CommonUtils',
	'components/heading',
	'components/charts/treemap',
	'components/reports/reportDrilldown',
	'knockout',
], function (
	view,
	TreemapReport,
	constants,
	commonUtils
) {
	class ConditionEra extends TreemapReport {
		constructor(params) {
			super(params);

			this.name = 'Condition Era'; // header
		}

		get aggProperty() {
			return constants.aggProperties.byLengthOfEra;
		}
	}

	return commonUtils.build('report-condition-era', ConditionEra, view);
});

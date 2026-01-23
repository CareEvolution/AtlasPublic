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
	class DrugEra extends TreemapReport {
		constructor(params) {
			super(params);

			this.name = 'Drug Era'; // header

			this.byLengthOfEra = true;
		}

		get aggProperty() {
			return constants.aggProperties.byLengthOfEra;
		}
	}

	return commonUtils.build('report-drug-era', DrugEra, view);
});

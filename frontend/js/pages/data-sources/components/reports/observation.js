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
	class Observation extends TreemapReport {
		constructor(params) {
			super(params);

			this.name = 'Observation'; // header

			this.byFrequency = true;
			this.byType = true;
			this.byValueAsConcept = true;
			this.byQualifier = true;
		}

		get aggProperty() {
			return constants.aggProperties.byPerson;
		}
	}

	return commonUtils.build('report-observation', Observation, view);
});

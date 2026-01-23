define([
	'knockout',
	'components/Chart',
	'components/Component',
	'atlascharts',
	'text!components/charts/chart.html',
	'utils/CommonUtils',
	'utils/ChartUtils'
], function (
	ko,
	Chart,
	Component,
	atlascharts,
	view,
	commonUtils,
	ChartUtils
) {
	class Histogram extends Chart {
		constructor(params, element) {
			super(params, element);
			this.renderer = new atlascharts.histogram();
			if(params.format && !params.format.getTooltipBuilder) {
				params.format.getTooltipBuilder = ChartUtils.getHistogramXYValueTooltip;
			}
		}

	}

	return commonUtils.build('histogram', Histogram, view);
});

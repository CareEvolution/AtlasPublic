define([
	'knockout',
	'components/Chart',
	'atlascharts',
	'd3',
	'text!components/charts/chart.html',
	'utils/CommonUtils',
	'utils/ChartUtils',
], function (
	ko,
	Chart,
	atlascharts,
	d3,
	view,
	commonUtils,
	ChartUtils,
) {
	class Trellisline extends Chart {
		constructor(params, element) {
			super(params, element);
			this.renderer = new atlascharts.trellisline();
		}

		prepareData(rawData) {
			const trellisData = ChartUtils.normalizeArray(rawData);
			if (!trellisData.empty) {
				const minYear = d3.min(trellisData.xCalendarYear),
					maxYear = d3.max(trellisData.xCalendarYear);

				const seriesInitializer = function (tName, sName, x, y) {
					return {
						trellisName: tName,
						seriesName: sName,
						xCalendarYear: x,
						yPrevalence1000Pp: y
					};
				};

				// map data into chartable form
				const normalizedSeries = trellisData.trellisName.map(function (d, i) {
					const item = {};
					const container = this;
					Object.keys(container).forEach(function (p) {
						item[p] = container[p][i];
					});
					return item;
				}, trellisData);

				const dataByDecile = d3
					.groups(
						normalizedSeries,
						(d) => d.trellisName,
						(d) => d.seriesName
					)
					.sort(
						(a, b) => a.xCalendarYear - b.xCalendarYear
					);

				// fill in gaps
				const yearRange = d3.range(minYear, maxYear, 1);
				let yearData = {};

				dataByDecile.forEach(function (trellis) {
					trellis[1].forEach(function (series) {
						series[1] = yearRange.map(function (year) {
							yearData = series[1].filter(function (f) {
								return f.xCalendarYear === year;
							})[0] || seriesInitializer(trellis[0], series[0], year, 0);
							yearData.date = new Date(year, 0, 1);
							return yearData;
						});
					});
				});

				return dataByDecile;
			}
			return null;
		}
	}

	return commonUtils.build('trellisline', Trellisline, view);
});

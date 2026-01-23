define([
	'knockout',
	'text!./datadensity.html',
	'd3',
	'utils/CommonUtils',
	'utils/ChartUtils',
	'components/reports/classes/Report',
	'components/Component',
	'components/heading',
	'components/charts/boxplot',
	'components/charts/line',
], function (
	ko,
	view,
	d3,
	commonUtils,
	ChartUtils,
	Report
) {
	class datadensity extends Report {
		constructor(params) {
			super(params);

			this.name = 'DataDensity';
			this.totalLineData = ko.observable();
			this.recordsperpersonLineData = ko.observable();
			this.conceptsPerPersonData = ko.observable();

			const wholeNumberFormatter = (value) => {
				return d3.format('d')(value);
			};

			const decimalNumberFormatter = (value) => {
				return value < 1
					? d3.format('.3f')(value)
					: d3.formatPrefix('.3s', value)(value);
			};

			this.chartFormats = {
				totalLine: {
					xScale: null,
					xFormat: d3.timeFormat('%m/%Y'),
					yFormat: wholeNumberFormatter,
					tickFormat: d3.timeFormat('%m/%Y'),
					xValue: 'xCalendarMonth',
					yValue: 'yRecordCount',
					xLabel: ko.i18n(
						'dataSources.datadensityReport.year',
						'Year'
					),
					yLabel: ko.i18n(
						'dataSources.datadensityReport.numberOfRecords',
						'# of Records'
					),
					showLegend: true,
					getTooltipBuilder: ChartUtils.getSeriesXYValueTooltip(
						ChartUtils.formatWholeNumber
					),
				},
				recordsperpersonLine: {
					xScale: null,
					xFormat: d3.timeFormat('%m/%Y'),
					yFormat: wholeNumberFormatter,
					tickFormat: d3.timeFormat('%m/%Y'),
					xValue: 'xCalendarMonth',
					yValue: 'yRecordCount',
					xLabel: ko.i18n(
						'dataSources.datadensityReport.year',
						'Year'
					),
					yLabel: ko.i18n(
						'dataSources.datadensityReport.recordsPerPerson',
						'Records Per Person'
					),
					showLegend: true,
					getTooltipBuilder: ChartUtils.getSeriesXYValueTooltip(
						decimalNumberFormatter
					),
				},
				conceptsPerPerson: {
					yMax: 0,
					yFormat: d3.format(',.1s'),
					xLabel: ko.i18n(
						'dataSources.datadensityReport.conceptType',
						'Concept Type'
					),
					yLabel: ko.i18n(
						'dataSources.datadensityReport.conceptsPerPerson',
						'Concepts per Person'
					),
					valueFormatter: d3.format('d'),
				},
			};

			this.loadData();
		}

		parseData({ data }) {
			const totalLine = this.parseLineData(data.totalRecords);
			const recordsPerPerson = this.parseLineData(data.recordsPerPerson);

			this.totalLineData(totalLine.data);
			this.chartFormats.totalLine.xScale = totalLine.xScale;
			this.recordsperpersonLineData(recordsPerPerson.data);
			this.chartFormats.recordsperpersonLine.xScale =
				recordsPerPerson.xScale;

			if (data.conceptsPerPerson) {
				const conceptsSeries = [];
				const conceptsData = ChartUtils.normalizeArray(
					data.conceptsPerPerson
				);
				for (let i = 0; i < conceptsData.category.length; i++) {
					conceptsSeries.push({
						Category: conceptsData.category[i],
						min: conceptsData.minValue[i],
						max: conceptsData.maxValue[i],
						median: conceptsData.medianValue[i],
						LIF: conceptsData.p10Value[i],
						q1: conceptsData.p25Value[i],
						q3: conceptsData.p75Value[i],
						UIF: conceptsData.p90Value[i],
					});
				}
				conceptsSeries.sort(ChartUtils.alphabeticBoxPlotSort);
				this.conceptsPerPersonData(conceptsSeries);
				this.chartFormats.conceptsPerPerson.yMax = d3.max(
					conceptsData.p90Value
				);
			} else {
				this.conceptsPerPersonData(null);
				this.chartFormats.conceptsPerPerson.yMax = 0;
			}
		}

		parseLineData(rawData) {
			const result = {
				data: null,
				xScale: null,
			};

			if (rawData) {
				// convert yyyymm to date
				rawData.forEach(function (d, i, ar) {
					const v = d.xCalendarMonth;
					ar[i].xCalendarMonth = new Date(
						Math.floor(v / 100),
						(v % 100) - 1,
						1
					);
				});

				// nest dataframe data into key->values pair
				const parsedData = d3
					.groups(rawData, (d) => d.seriesName)
					.map((d) => {
						return {
							name: d[0],
							values: d[1],
						};
					});

				result.data = parsedData;
				result.xScale = d3
					.scaleTime()
					.domain(d3.extent(rawData, (d) => d.xCalendarMonth));
			}

			return result;
		}
	}

	return commonUtils.build('report-datadensity', datadensity, view);
});

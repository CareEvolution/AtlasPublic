define([
	'knockout',
	'atlascharts',
	'utils/ChartUtils',
	'utils/CommonUtils',
	'const',
	'services/AuthAPI',
	'./Report',
	'faceted-datatable'
], function (
	ko,
	atlascharts,
	ChartUtils,
	commonUtils,
	constants,
	authApi,
	Report,
) {
	class TreemapReport extends Report {
		// abstract, no need to define component name here

		constructor(params) {

			super(params);
			this.treeData = ko.observable();
			this.tableData = ko.observable();
			this.currentConcept = ko.observable();
			this.currentConceptSubscription = this.currentConcept.subscribe(c => {
				c && this.context.loadingDrilldown(true);
			});

			this.byFrequency = false;
			this.byUnit = false;
			this.byType = false;
			this.byValueAsConcept = false;
			this.byOperator = false;
			this.byQualifier = false;
			this.byLengthOfEra = false;
			this.handleConceptClick = node => {
				this.currentConcept(node);
			};

			function getcolorrange() {
				return constants.treemapGradient;
			}
			this.chartFormats = {
				treemap: {
					useTip: true,
					minimumArea: 50,
					onclick: this.handleConceptClick,
					getsizevalue: node => node.num_persons,
					getcolorvalue: node => node.agg_value,
					getcolorrange: getcolorrange,
					getcontent: (node) => {
						const steps = node.path.split('||');
						const i = steps.length - 1;
						return `<div class="pathleaf">${steps[i]}</div>
            <div class="pathleafstat">${ko.i18n('dataSources.prevalence', 'Prevalence')()}: ${ChartUtils.formatPercent(node.percent_persons)}</div>
            <div class="pathleafstat">${ko.i18n('dataSources.numberOfPeople', 'Number of People')()}: ${ChartUtils.formatComma(node.num_persons)}</div>
            <div class="pathleafstat">${ko.unwrap(this.aggProperty.description)}: ${ChartUtils.formatFixed(node.agg_value)}</div>
            `;
					},
					gettitle: (node) => {
						let title = '';
						const steps = node.path.split('||');
						const nonNASteps = steps.slice(steps.findIndex(step => step !== 'NA'));
						for (let i = 0; i < nonNASteps.length - 1; i++) {
							title += ' <div class="pathstep">' + Array(i + 1).join('&nbsp;&nbsp') + nonNASteps[i] + ' </div>';
						}
						return title;
					}
				},
				table: {
					order: [2, 'desc'],
					onclick: this.handleConceptClick,
					buttons: ['colvis', 'copyHtml5', 'excelHtml5', 'csvHtml5', 'pdfHtml5'],
					autoWidth: false,
					createdRow: function (row) {
						$(row).addClass('table_selector');
					},
					columns: [{
							title: ko.i18n('columns.conceptId', 'Concept Id'),
							data: 'concept_id',
							className: 'treemap__tbl-col--narrow numeric'
						},
						{
							title: ko.i18n('columns.name', 'Name'),
							data: 'name'
						},
						{
							title: ko.i18n('columns.personCount', 'Person Count'),
							data: 'num_persons',
							className: 'treemap__tbl-col--narrow numeric',
							orderSequence: ['desc', 'asc']
						},
						{
							title: ko.i18n('columns.prevalence', 'Prevalence'),
							data: 'percent_persons',
							className: 'treemap__tbl-col--narrow numeric',
							orderSequence: ['desc', 'asc']
						},
						{
							title: this.aggProperty.description,
							data: 'agg_value',
							className: 'treemap__tbl-col--narrow numeric',
							orderSequence: ['desc', 'asc']
						}
					],
					lengthChange: false,
					deferRender: true,
					destroy: true,
					...commonUtils.getTableOptions('L'),
				},
			};
			// to pass down to drilldown
			this.currentReport = params.report;
			this.getData()
				.then(() => {
					// in order to get jquery working, we should set isLoading here instead of .finally block
					this.context.loadingReport(false);
					this.isLoading(false);
				});
		}

		get aggProperty() {
			return {
				name: '',
				description: '',
			};
		};

		selectTab(tab) {

		}

		dispose() {
			this.currentConceptSubscription.dispose();
			super.dispose();
		}

		parseData({
			data
		}) {
			const normalizedData = atlascharts.chart.normalizeDataframe(ChartUtils.normalizeArray(data, true));
			const tableData = [];

			if (!normalizedData.empty) {
				const distinctConceptIds = new Set([]);
				// Make the values unique per https://github.com/OHDSI/Atlas/issues/913
				normalizedData.conceptPath.forEach((d, i) => {
					if (!distinctConceptIds.has(normalizedData.conceptId[i])) {
						distinctConceptIds.add(normalizedData.conceptId[i]);
						const pathParts = d.split('||');
						tableData.push({
							concept_id: normalizedData.conceptId[i],
							name: pathParts[pathParts.length - 1],
							ingredient: pathParts[3],
							num_persons: ChartUtils.formatComma(normalizedData.numPersons[i]),
							percent_persons: ChartUtils.formatPercent(normalizedData.percentPersons[i]),
							agg_value: ChartUtils.formatFixed(normalizedData[this.aggProperty.name][i])
						});
					}
				});
				this.treeData(normalizedData);
			} else {
				this.treeData(null);
			}
			
			this.tableData(tableData);

			return {
				data
			};
		}

		getData() {
			this.currentConcept(null);
			const response = super.getData();
			response
				.then((data) => this.parseData(data));

			return response;
		}

	}

	return TreemapReport;
});
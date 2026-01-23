define([
	'knockout',
	'components/Component',
	'utils/AutoBind',
	'utils/Clipboard',
	'components/cohortbuilder/CohortExpression',
	'services/CohortDefinition',
	'utils/CommonUtils',
	'services/Tags',
	'text!./cohort-definition-exporter.html',
	'utilities/sql',
	'databindings/expressionCartoonBinding',
], function (
	ko,
	Component,
	AutoBind,
	Clipboard,
	CohortExpression,
	cohortDefinitionService,
	commonUtils,
	tagService,
	view
) {

	function conceptSetSorter(a, b) {
		const textA = a.name().toUpperCase();
		const textB = b.name().toUpperCase();
		return textA < textB ? -1 : textA > textB ? 1 : 0;
	}

	const includeKeys = ['UseEventEnd'];
	function pruneJSON(key, value) {
		if (value === 0 || value || includeKeys.includes(key)) {
			return value;
		}
	}

	class CohortDefinitionExporter extends AutoBind(Clipboard(Component)) {
		constructor(params) {
			super(params);

			this.currentCohortDefinition = params.cohortDefinition;
			this.hasAccess = params.hasAccess;
			this.delayedCartoonUpdate = ko.observable(null);
			this.exportTabMode = ko.observable('printfriendly');
			this.tabPath = ko.pureComputed(() => {
				const path = 'export/' + this.exportTabMode();
				return path;
			});
			this.subscriptions.push(
				this.exportTabMode.subscribe((val) => {
					if (val === 'cartoon') {
						setTimeout(() => {
							this.delayedCartoonUpdate('ready');
						}, 10);
					}
				})
			);

			this.sortedConceptSets = ko.pureComputed(() => {
				if (this.currentCohortDefinition() != null) {
					var clone = this.currentCohortDefinition()
						.expression()
						.ConceptSets()
						.slice(0);
					return clone.sort(conceptSetSorter);
				}
			});

			this._selectedCriteria = ko.observable();
			this.selectedCriteria = ko.pureComputed({
				write: (criteria) => {
					this._selectedCriteria(criteria);
					ko.tasks.runEarly();
				},
				read: () => this._selectedCriteria(),
			});

			this.modifiedJSON = '';

			Object.defineProperty(this, 'expressionJSON', {
				get: () => this.getExpressionJson(),
				set: (val) => {
					this.setExpressionJson(val);
				},
			});

			this.exportSqlService = this.exportSql;
			this.cohortDefinitionService = cohortDefinitionService;

			// print-friendly state
			this.printFriendlyHtml = ko.observable();
			this.printFriendlyLoading = ko.observable(false);

			this.refreshPrintFriendly();
		}

		async refreshPrintFriendly() {
			this.printFriendlyLoading(true);
			try {
				const printFriendlyHtml =
					await cohortDefinitionService.getCohortPrintFriendly(
						ko.toJS(this.currentCohortDefinition().expression())
					);
				this.printFriendlyHtml(printFriendlyHtml);
			} catch (error) {
				console.error('Problem loading print-friendly output.', error);
			} finally {
				this.printFriendlyLoading(false);
			}
		}

		getTagGroups() {
			const tags = this.currentCohortDefinition().tags();
			const sortedTags = tagService.getSortedTags(tags);
			return tagService.getTagGroups(sortedTags);
		}

		copyTextViewToClipboard() {
			const columns = [
				{
					title: ko.i18n('columns.conceptId', 'Concept Id'),
					data: 'CONCEPT_ID',
				},
				{
					title: ko.i18n('columns.conceptName', 'Concept Name'),
					data: 'CONCEPT_NAME',
				},
				{
					title: ko.i18n('columns.domain', 'Domain'),
					data: 'DOMAIN_ID',
				},
				{
					title: ko.i18n('columns.vocabulary', 'Vocabulary'),
					data: 'VOCABULARY_ID',
				},
			];
			let setsText = '';
			this.sortedConceptSets().forEach((set) => {
				setsText += '\n' + set.name() + '\n';
				columns.forEach((c) => {
					setsText += c.title() + '\t';
				});
				setsText += 'Excluded\tDescendants\tMapped' + '\n';
				set.expression.items().forEach((item) => {
					columns.forEach((c) => {
						setsText += item.concept[c.data] + '\t';
					});
					setsText += (item.isExcluded() ? 'YES' : 'NO') + '\t';
					setsText +=
						(item.includeDescendants() ? 'YES' : 'NO') + '\t';
					setsText += (item.includeMapped() ? 'YES' : 'NO') + '\n';
				});
			});
			this.copyToClipboard(
				'#btnCopyTextViewClipboard',
				'#copyTextViewMessage',
				setsText
			);
		}

		copyCohortExpressionJSONToClipboard() {
			this.copyToClipboard(
				'#btnCopyExpressionJSONClipboard',
				'#copyCohortExpressionJSONMessage'
			);
		}

		getCriteriaIndexComponent(data) {
			data = ko.utils.unwrapObservable(data);
			if (!data) return;
			if (data.hasOwnProperty('ConditionOccurrence'))
				return 'condition-occurrence-criteria-viewer';
			else if (data.hasOwnProperty('ConditionEra'))
				return 'condition-era-criteria-viewer';
			else if (data.hasOwnProperty('DrugExposure'))
				return 'drug-exposure-criteria-viewer';
			else if (data.hasOwnProperty('DrugEra'))
				return 'drug-era-criteria-viewer';
			else if (data.hasOwnProperty('DoseEra'))
				return 'dose-era-criteria-viewer';
			else if (data.hasOwnProperty('ProcedureOccurrence'))
				return 'procedure-occurrence-criteria-viewer';
			else if (data.hasOwnProperty('Observation'))
				return 'observation-criteria-viewer';
			else if (data.hasOwnProperty('VisitOccurrence'))
				return 'visit-occurrence-criteria-viewer';
			else if (data.hasOwnProperty('VisitDetail'))
				return 'visit-detail-criteria-viewer';
			else if (data.hasOwnProperty('DeviceExposure'))
				return 'device-exposure-criteria-viewer';
			else if (data.hasOwnProperty('Measurement'))
				return 'measurement-criteria-viewer';
			else if (data.hasOwnProperty('Specimen'))
				return 'specimen-criteria-viewer';
			else if (data.hasOwnProperty('ObservationPeriod'))
				return 'observation-period-criteria-viewer';
			else if (data.hasOwnProperty('PayerPlanPeriod'))
				return 'payer-plan-period-criteria-viewer';
			else if (data.hasOwnProperty('Death'))
				return 'death-criteria-viewer';
			else if (data.hasOwnProperty('LocationRegion'))
				return 'location-region-viewer';
			else return 'unknownCriteriaType';
		}

		getExpressionJson() {
			if (!this.currentCohortDefinition()) {
				return ko.toJSON(null);
			}
			return ko.toJSON(
				this.currentCohortDefinition().expression(),
				(key, value) => {
					// UseEventEnd is a speical case: always include this key in the result.
					if (
						value === 0 ||
						value ||
						['UseEventEnd'].indexOf(key) > -1
					) {
						return value;
					} else {
						return;
					}
				},
				2
			);
		}

		setExpressionJson(value) {
			this.modifiedJSON = value;
		}

		reload() {
			if (this.modifiedJSON.length > 0) {
				const updatedExpression = JSON.parse(this.modifiedJSON);
				this.currentCohortDefinition().expression(
					new CohortExpression(updatedExpression)
				);
			}
		}

		async exportSql({ expression = {} } = {}) {
			return await this.cohortDefinitionService.getSql(
				ko.toJS(expression, pruneJSON)
			);
		}
	}

	return commonUtils.build(
		'cohort-definition-exporter',
		CohortDefinitionExporter,
		view
	);
});
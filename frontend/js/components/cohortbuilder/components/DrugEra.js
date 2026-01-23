define([
	'knockout',
	'../options',
	'../utils',
	'../InputTypes/Range',
	'../InputTypes/DateAdjustment',
	'../CriteriaGroup',
	'text!./DrugEraTemplate.html',
	'../const',
], function (
	ko,
	options,
	utils,
	Range,
	DateAdjustment,
	CriteriaGroup,
	template,
	constants
) {
	function DrugEraViewModel(params) {
		var self = this;
		self.expression = ko.utils.unwrapObservable(params.expression);
		self.Criteria = params.criteria.DrugEra;
		self.options = options;
		self.hideClose = params.hideClose;

		self.removeCriteria = function () {
			params.deleteCallback();
		};

		self.addActions = [
			{
				...constants.drugAttributes.addFirstDiagnosis,
				selected: false,
				action: function () {
					if (self.Criteria.First() == null)
						self.Criteria.First(true);
					else self.Criteria.First(null);
				},
				attributeName: 'First',
			},
			{
				...constants.drugAttributes.addAgeAtStart,
				selected: false,
				action: function () {
					if (self.Criteria.AgeAtStart() == null)
						self.Criteria.AgeAtStart(new Range());
					else self.Criteria.AgeAtStart(null);
				},
				attributeName: 'AgeAtStart',
			},
			{
				...constants.drugAttributes.addAgeAtEnd,
				selected: false,
				action: function () {
					if (self.Criteria.AgeAtEnd() == null)
						self.Criteria.AgeAtEnd(new Range());
					else self.Criteria.AgeAtEnd(null);
				},
				attributeName: 'AgeAtEnd',
			},
			{
				...constants.drugAttributes.addGender,
				selected: false,
				action: function () {
					if (self.Criteria.Gender() == null)
						self.Criteria.Gender(ko.observableArray());
					else self.Criteria.Gender(null);
				},
				attributeName: 'Gender',
			},
			{
				...constants.drugAttributes.addStartDate,
				selected: false,
				action: function () {
					if (self.Criteria.EraStartDate() == null)
						self.Criteria.EraStartDate(
							new Range({
								Op: 'lt',
							})
						);
					else self.Criteria.EraStartDate(null);
				},
				attributeName: 'EraStartDate',
			},
			{
				...constants.drugAttributes.addEndDate,
				selected: false,
				action: function () {
					if (self.Criteria.EraEndDate() == null)
						self.Criteria.EraEndDate(
							new Range({
								Op: 'lt',
							})
						);
					else self.Criteria.EraEndDate(null);
				},
				attributeName: 'EraEndDate',
			},
			{
				...constants.eraAttributes.addDateAdjustment,
				selected: false,
				action: function () {
					if (self.Criteria.DateAdjustment() == null)
						self.Criteria.DateAdjustment(new DateAdjustment());
					else self.Criteria.DateAdjustment(null);
				},
				attributeName: 'DateAdjustment',
			},
			{
				...constants.drugAttributes.addLength,
				selected: false,
				action: function () {
					if (self.Criteria.EraLength() == null)
						self.Criteria.EraLength(new Range());
					else self.Criteria.EraLength(null);
				},
				attributeName: 'EraLength',
			},
			{
				...constants.drugAttributes.addConditonCount,
				selected: false,
				action: function () {
					if (self.Criteria.OccurrenceCount() == null)
						self.Criteria.OccurrenceCount(new Range());
					else self.Criteria.OccurrenceCount(null);
				},
				attributeName: 'OccurrenceCount',
			},
			{
				...constants.drugAttributes.addNested,
				selected: false,
				action: function () {
					if (self.Criteria.CorrelatedCriteria() == null)
						self.Criteria.CorrelatedCriteria(
							new CriteriaGroup(null, self.expression.ConceptSets)
						);
					else self.Criteria.CorrelatedCriteria(null);
				},
				attributeName: 'CorrelatedCriteria',
			},
		];

		self.removeCriterion = function (propertyName) {
			self.Criteria[propertyName](null);
		};
		self.indexMessage = ko.i18nformat(
			'components.conditionDrug.indexDataText',
			'The index date refers to the drug era of <%= conceptSetName %>.',
			{
				conceptSetName: ko.pureComputed(() =>
					utils.getConceptSetName(
						self.Criteria.CodesetId,
						self.expression.ConceptSets,
						ko.i18n('components.conditionDrug.anyDrug', 'Any Drug')
					)
				),
			}
		);
	}

	// return compoonent definition
	return {
		viewModel: DrugEraViewModel,
		template: template,
	};
});

define([
	'knockout',
	'../options',
	'../utils',
	'../InputTypes/Range',
	'../InputTypes/DateAdjustment',
	'../CriteriaGroup',
	'text!./DoseEraTemplate.html',
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
	function DoseEraViewModel(params) {
		var self = this;
		self.expression = ko.utils.unwrapObservable(params.expression);
		self.Criteria = params.criteria.DoseEra;
		self.options = options;
		self.hideClose = params.hideClose;

		self.removeCriteria = function () {
			params.deleteCallback();
		};

		self.addActions = [
			{
				...constants.doseAttributes.addFirstDiagnosis,
				selected: false,
				action: function () {
					if (self.Criteria.First() == null)
						self.Criteria.First(true);
					else self.Criteria.First(null);
				},
				attributeName: 'First',
			},
			{
				...constants.doseAttributes.addAgeAtStart,
				selected: false,
				action: function () {
					if (self.Criteria.AgeAtStart() == null)
						self.Criteria.AgeAtStart(new Range());
					else self.Criteria.AgeAtStart(null);
				},
				attributeName: 'AgeAtStart',
			},
			{
				...constants.doseAttributes.addAgeAtEnd,
				selected: false,
				action: function () {
					if (self.Criteria.AgeAtEnd() == null)
						self.Criteria.AgeAtEnd(new Range());
					else self.Criteria.AgeAtEnd(null);
				},
				attributeName: 'AgeAtEnd',
			},
			{
				...constants.doseAttributes.addGender,
				selected: false,
				action: function () {
					if (self.Criteria.Gender() == null)
						self.Criteria.Gender(ko.observableArray());
					else self.Criteria.Gender(null);
				},
				attributeName: 'Gender',
			},
			{
				...constants.doseAttributes.addStartDate,
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
				...constants.doseAttributes.addEndDate,
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
				...constants.doseAttributes.addDateAdjustment,
				selected: false,
				action: function () {
					if (self.Criteria.DateAdjustment() == null)
						self.Criteria.DateAdjustment(new DateAdjustment());
					else self.Criteria.DateAdjustment(null);
				},
				attributeName: 'DateAdjustment',
			},
			{
				...constants.doseAttributes.addUnit,
				selected: false,
				action: function () {
					if (self.Criteria.Unit() == null)
						self.Criteria.Unit(ko.observableArray());
					else self.Criteria.Unit(null);
				},
				attributeName: 'Unit',
			},
			{
				...constants.doseAttributes.addLength,
				selected: false,
				action: function () {
					if (self.Criteria.EraLength() == null)
						self.Criteria.EraLength(new Range());
					else self.Criteria.EraLength(null);
				},
				attributeName: 'EraLength',
			},
			{
				...constants.doseAttributes.addValue,
				selected: false,
				action: function () {
					if (self.Criteria.DoseValue() == null)
						self.Criteria.DoseValue(new Range());
					else self.Criteria.DoseValue(null);
				},
				attributeName: 'DoseValue',
			},
			{
				...constants.doseAttributes.addNested,
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
			'components.conditionDose.indexDataText',
			'The index date refers to the dose era of <%= conceptSetName %>.',
			{
				conceptSetName: ko.pureComputed(() =>
					utils.getConceptSetName(
						self.Criteria.CodesetId,
						self.expression.ConceptSets,
						ko.i18n(
							'components.conditionDose.anyDoseEra',
							'Any Dose Era'
						)
					)
				),
			}
		);
	}

	// return compoonent definition
	return {
		viewModel: DoseEraViewModel,
		template: template,
	};
});

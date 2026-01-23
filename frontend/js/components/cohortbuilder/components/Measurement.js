define([
	'knockout',
	'../options',
	'../utils',
	'../InputTypes/Range',
	'../InputTypes/DateAdjustment',
	'../CriteriaGroup',
	'text!./MeasurementTemplate.html',
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
	function MeasurementViewModel(params) {
		var self = this;

		self.expression = ko.utils.unwrapObservable(params.expression);
		self.Criteria = params.criteria.Measurement;
		self.options = options;
		self.hideClose = params.hideClose;

		self.removeCriteria = function () {
			params.deleteCallback();
		};

		self.addActions = [
			{
				...constants.measurementAttributes.addFirstDiagnosis,
				selected: false,
				action: function () {
					if (self.Criteria.First() == null)
						self.Criteria.First(true);
					else self.Criteria.First(null);
				},
				attributeName: 'First',
			},
			{
				...constants.measurementAttributes.addAge,
				selected: false,
				action: function () {
					if (self.Criteria.Age() == null)
						self.Criteria.Age(new Range());
					else self.Criteria.Age(null);
				},
				attributeName: 'Age',
			},
			{
				...constants.measurementAttributes.addGender,
				selected: false,
				action: function () {
					if (self.Criteria.Gender() == null)
						self.Criteria.Gender(ko.observableArray());
					else self.Criteria.Gender(null);
				},
				attributeName: 'Gender',
			},
			{
				...constants.measurementAttributes.addDate,
				selected: false,
				action: function () {
					if (self.Criteria.OccurrenceStartDate() == null)
						self.Criteria.OccurrenceStartDate(
							new Range({
								Op: 'lt',
							})
						);
					else self.Criteria.OccurrenceStartDate(null);
				},
				attributeName: 'OccurrenceStartDate',
			},
			{
				...constants.measurementAttributes.addDateAdjustment,
				selected: false,
				action: function () {
					if (self.Criteria.DateAdjustment() == null)
						self.Criteria.DateAdjustment(new DateAdjustment());
					else self.Criteria.DateAdjustment(null);
				},
				attributeName: 'DateAdjustment',
			},
			{
				...constants.measurementAttributes.addType,
				selected: false,
				action: function () {
					if (self.Criteria.MeasurementType() == null)
						self.Criteria.MeasurementType(ko.observableArray());
					else self.Criteria.MeasurementType(null);
				},
				attributeName: 'MeasurementType',
			},
			{
				...constants.measurementAttributes.addVisit,
				selected: false,
				action: function () {
					if (self.Criteria.VisitType() == null)
						self.Criteria.VisitType(ko.observableArray());
					else self.Criteria.VisitType(null);
				},
				attributeName: 'VisitType',
			},
			{
				...constants.measurementAttributes.addOperator,
				selected: false,
				action: function () {
					if (self.Criteria.Operator() == null)
						self.Criteria.Operator(ko.observableArray());
					else self.Criteria.Operator(null);
				},
				attributeName: 'Operator',
			},
			{
				...constants.measurementAttributes.addValue,
				selected: false,
				action: function () {
					if (self.Criteria.ValueAsNumber() == null)
						self.Criteria.ValueAsNumber(new Range());
					else self.Criteria.ValueAsNumber(null);
				},
				attributeName: 'ValueAsNumber',
			},
			{
				...constants.measurementAttributes.addValueAsConcept,
				selected: false,
				action: function () {
					if (self.Criteria.ValueAsConcept() == null)
						self.Criteria.ValueAsConcept(ko.observableArray());
					else self.Criteria.ValueAsConcept(null);
				},
				attributeName: 'ValueAsConcept',
			},
			{
				...constants.measurementAttributes.addUnit,
				selected: false,
				action: function () {
					if (self.Criteria.Unit() == null)
						self.Criteria.Unit(ko.observableArray());
					else self.Criteria.Unit(null);
				},
				attributeName: 'Unit',
			},
			{
				...constants.measurementAttributes.addAbnormal,
				selected: false,
				action: function () {
					if (self.Criteria.Abnormal() == null)
						self.Criteria.Abnormal(true);
					else self.Criteria.Abnormal(null);
				},
				attributeName: 'Abnormal',
			},
			{
				...constants.measurementAttributes.addRangeLow,
				selected: false,
				action: function () {
					if (self.Criteria.RangeLow() == null)
						self.Criteria.RangeLow(new Range());
					else self.Criteria.RangeLow(null);
				},
				attributeName: 'RangeLow',
			},
			{
				...constants.measurementAttributes.addRangeHigh,
				selected: false,
				action: function () {
					if (self.Criteria.RangeHigh() == null)
						self.Criteria.RangeHigh(new Range());
					else self.Criteria.RangeHigh(null);
				},
				attributeName: 'RangeHigh',
			},
			{
				...constants.measurementAttributes.addRangeLowRatio,
				selected: false,
				action: function () {
					if (self.Criteria.RangeLowRatio() == null)
						self.Criteria.RangeLowRatio(new Range());
					else self.Criteria.RangeLowRatio(null);
				},
				attributeName: 'RangeLowRatio',
			},
			{
				...constants.measurementAttributes.addRangeHighRatio,
				selected: false,
				action: function () {
					if (self.Criteria.RangeHighRatio() == null)
						self.Criteria.RangeHighRatio(new Range());
					else self.Criteria.RangeHighRatio(null);
				},
				attributeName: 'RangeHighRatio',
			},
			{
				...constants.measurementAttributes.addProviderSpecialty,
				selected: false,
				action: function () {
					if (self.Criteria.ProviderSpecialty() == null)
						self.Criteria.ProviderSpecialty(ko.observableArray());
					else self.Criteria.ProviderSpecialty(null);
				},
				attributeName: 'ProviderSpecialty',
			},
			{
				...constants.measurementAttributes.addSourceConcept,
				selected: false,
				action: function () {
					if (self.Criteria.MeasurementSourceConcept() == null)
						self.Criteria.MeasurementSourceConcept(ko.observable());
					else self.Criteria.MeasurementSourceConcept(null);
				},
				attributeName: 'MeasurementSourceConcept',
			},
			{
				...constants.measurementAttributes.addNested,
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

		self.indexMessage = ko.pureComputed(() => {
			var conceptSetName = utils.getConceptSetName(
				self.Criteria.CodesetId,
				self.expression.ConceptSets,
				''
			);
			return `${conceptSetName}.`;
		});

		self.indexMessage = ko.i18nformat(
			'components.conditionMeasurement.indexDataText',
			'The index date refers to the measurement of <%= conceptSetName %>.',
			{
				conceptSetName: ko.pureComputed(() =>
					utils.getConceptSetName(
						self.Criteria.CodesetId,
						self.expression.ConceptSets,
						ko.i18n(
							'components.conditionMeasurement.anyMeasurement',
							'Any Measurement'
						)
					)
				),
			}
		);
	}

	// return compoonent definition
	return {
		viewModel: MeasurementViewModel,
		template: template,
	};
});

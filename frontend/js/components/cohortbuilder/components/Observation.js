define([
	'knockout',
	'../options',
	'../utils',
	'../InputTypes/Range',
	'../InputTypes/DateAdjustment',
	'../InputTypes/Text',
	'../CriteriaGroup',
	'text!./ObservationTemplate.html',
	'../const',
], function (
	ko,
	options,
	utils,
	Range,
	DateAdjustment,
	Text,
	CriteriaGroup,
	template,
	constants
) {
	function ObservationViewModel(params) {
		var self = this;
		self.addActions = [
			{
				...constants.observationAttributes.addFirstDiagnosis,
				selected: false,
				action: function () {
					if (self.Criteria.First() == null)
						self.Criteria.First(true);
					else self.Criteria.First(null);
				},
				attributeName: 'First',
			},
			{
				...constants.observationAttributes.addAge,
				selected: false,
				action: function () {
					if (self.Criteria.Age() == null)
						self.Criteria.Age(new Range());
					else self.Criteria.Age(null);
				},
				attributeName: 'Age',
			},
			{
				...constants.observationAttributes.addGender,
				selected: false,
				action: function () {
					if (self.Criteria.Gender() == null)
						self.Criteria.Gender(ko.observableArray());
					else self.Criteria.Gender(null);
				},
				attributeName: 'Gender',
			},
			{
				...constants.observationAttributes.addDate,
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
				...constants.observationAttributes.addDateAdjustment,
				selected: false,
				action: function () {
					if (self.Criteria.DateAdjustment() == null)
						self.Criteria.DateAdjustment(new DateAdjustment());
					else self.Criteria.DateAdjustment(null);
				},
				attributeName: 'DateAdjustment',
			},
			{
				...constants.observationAttributes.addType,
				selected: false,
				action: function () {
					if (self.Criteria.ObservationType() == null)
						self.Criteria.ObservationType(ko.observableArray());
					else self.Criteria.ObservationType(null);
				},
				attributeName: 'ObservationType',
			},
			{
				...constants.observationAttributes.addVisit,
				selected: false,
				action: function () {
					if (self.Criteria.VisitType() == null)
						self.Criteria.VisitType(ko.observableArray());
					else self.Criteria.VisitType(null);
				},
				attributeName: 'VisitType',
			},
			{
				...constants.observationAttributes.addValue,
				selected: false,
				action: function () {
					if (self.Criteria.ValueAsNumber() == null)
						self.Criteria.ValueAsNumber(
							new Range({
								Op: 'lt',
							})
						);
					else self.Criteria.ValueAsNumber(null);
				},
				attributeName: 'ValueAsNumber',
			},
			{
				...constants.observationAttributes.addValueAsString,
				selected: false,
				action: function () {
					if (self.Criteria.ValueAsString() == null)
						self.Criteria.ValueAsString(
							new Text({
								Op: 'contains',
							})
						);
					else self.Criteria.ValueAsString(null);
				},
				attributeName: 'ValueAsString',
			},
			{
				...constants.observationAttributes.addValueAsConcept,
				selected: false,
				action: function () {
					if (self.Criteria.ValueAsConcept() == null)
						self.Criteria.ValueAsConcept(ko.observableArray());
					else self.Criteria.ValueAsConcept(null);
				},
				attributeName: 'ValueAsConcept',
			},
			{
				...constants.observationAttributes.addQualifier,
				selected: false,
				action: function () {
					if (self.Criteria.Qualifier() == null)
						self.Criteria.Qualifier(ko.observableArray());
					else self.Criteria.Qualifier(null);
				},
				attributeName: 'Qualifier',
			},
			{
				...constants.observationAttributes.addUnit,
				selected: false,
				action: function () {
					if (self.Criteria.Unit() == null)
						self.Criteria.Unit(ko.observableArray());
					else self.Criteria.Unit(null);
				},
				attributeName: 'Unit',
			},
			{
				...constants.observationAttributes.addSourceConcept,
				selected: false,
				action: function () {
					if (self.Criteria.ObservationSourceConcept() == null)
						self.Criteria.ObservationSourceConcept(ko.observable());
					else self.Criteria.ObservationSourceConcept(null);
				},
				attributeName: 'ObservationSourceConcept',
			},
			{
				...constants.observationAttributes.addProviderSpecialty,
				selected: false,
				action: function () {
					if (self.Criteria.ProviderSpecialty() == null)
						self.Criteria.ProviderSpecialty(ko.observableArray());
					else self.Criteria.ProviderSpecialty(null);
				},
				attributeName: 'ProviderSpecialty',
			},
			{
				...constants.observationAttributes.addNested,
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

		self.expression = ko.utils.unwrapObservable(params.expression);
		self.Criteria = params.criteria.Observation;
		self.options = options;
		self.hideClose = params.hideClose;

		self.removeCriteria = function () {
			params.deleteCallback();
		};

		self.removeCriterion = function (propertyName) {
			self.Criteria[propertyName](null);
		};

		self.indexMessage = ko.i18nformat(
			'components.conditionObservation.indexDataText',
			'The index date refers to the observation of <%= conceptSetName %>.',
			{
				conceptSetName: ko.pureComputed(() =>
					utils.getConceptSetName(
						self.Criteria.CodesetId,
						self.expression.ConceptSets,
						ko.i18n(
							'components.conditionObservation.anyObservation',
							'Any Observation'
						)
					)
				),
			}
		);
	}

	// return compoonent definition
	return {
		viewModel: ObservationViewModel,
		template: template,
	};
});

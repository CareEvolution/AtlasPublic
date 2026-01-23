define([
	'knockout',
	'../options',
	'../utils',
	'../InputTypes/Range',
	'../InputTypes/DateAdjustment',
	'../CriteriaGroup',
	'text!./ProcedureOccurrenceTemplate.html',
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
	function ProcedureOccurrenceViewModel(params) {
		var self = this;
		self.addActions = [
			{
				...constants.procedureOccurrenceAttributes.addFirstDiagnosis,
				selected: false,
				action: function () {
					if (self.Criteria.First() == null)
						self.Criteria.First(true);
					else self.Criteria.First(null);
				},
				attributeName: 'First',
			},
			{
				...constants.procedureOccurrenceAttributes.addAge,
				selected: false,
				action: function () {
					if (self.Criteria.Age() == null)
						self.Criteria.Age(new Range());
					else self.Criteria.Age(null);
				},
				attributeName: 'Age',
			},
			{
				...constants.procedureOccurrenceAttributes.addGender,
				selected: false,
				action: function () {
					if (self.Criteria.Gender() == null)
						self.Criteria.Gender(ko.observableArray());
					else self.Criteria.Gender(null);
				},
				attributeName: 'Gender',
			},
			{
				...constants.procedureOccurrenceAttributes.addDate,
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
				...constants.procedureOccurrenceAttributes.addDateAdjustment,
				selected: false,
				action: function () {
					if (self.Criteria.DateAdjustment() == null)
						self.Criteria.DateAdjustment(new DateAdjustment());
					else self.Criteria.DateAdjustment(null);
				},
				attributeName: 'DateAdjustment',
			},
			{
				...constants.procedureOccurrenceAttributes.addType,
				selected: false,
				action: function () {
					if (self.Criteria.ProcedureType() == null)
						self.Criteria.ProcedureType(ko.observableArray());
					else self.Criteria.ProcedureType(null);
				},
				attributeName: 'ProcedureType',
			},
			{
				...constants.procedureOccurrenceAttributes.addVisit,
				selected: false,
				action: function () {
					if (self.Criteria.VisitType() == null)
						self.Criteria.VisitType(ko.observableArray());
					else self.Criteria.VisitType(null);
				},
				attributeName: 'VisitType',
			},
			{
				...constants.procedureOccurrenceAttributes.addModifier,
				selected: false,
				action: function () {
					if (self.Criteria.Modifier() == null)
						self.Criteria.Modifier(ko.observableArray());
					else self.Criteria.Modifier(null);
				},
				attributeName: 'Modifier',
			},
			{
				...constants.procedureOccurrenceAttributes.addQuantity,
				selected: false,
				action: function () {
					if (self.Criteria.Quantity() == null)
						self.Criteria.Quantity(
							new Range({
								Op: 'lt',
							})
						);
					else self.Criteria.Quantity(null);
				},
				attributeName: 'Quantity',
			},
			{
				...constants.procedureOccurrenceAttributes.addSourceConcept,
				selected: false,
				action: function () {
					if (self.Criteria.ProcedureSourceConcept() == null)
						self.Criteria.ProcedureSourceConcept(ko.observable());
					else self.Criteria.ProcedureSourceConcept(null);
				},
				attributeName: 'ProcedureSourceConcept',
			},
			{
				...constants.procedureOccurrenceAttributes.addProviderSpecialty,
				selected: false,
				action: function () {
					if (self.Criteria.ProviderSpecialty() == null)
						self.Criteria.ProviderSpecialty(ko.observableArray());
					else self.Criteria.ProviderSpecialty(null);
				},
				attributeName: 'ProviderSpecialty',
			},
			{
				...constants.procedureOccurrenceAttributes.addNested,
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
		self.Criteria = params.criteria.ProcedureOccurrence;
		self.options = options;
		self.hideClose = params.hideClose;

		self.removeCriteria = function () {
			params.deleteCallback();
		};

		self.removeCriterion = function (propertyName) {
			self.Criteria[propertyName](null);
		};

		self.indexMessage = ko.i18nformat(
			'components.conditionProcedureOccurrence.indexDataText',
			'The index date refers to the procedure of <%= conceptSetName %>.',
			{
				conceptSetName: ko.pureComputed(() =>
					utils.getConceptSetName(
						self.Criteria.CodesetId,
						self.expression.ConceptSets,
						ko.i18n(
							'components.conditionProcedureOccurrence.anyProcedure',
							'Any Procedure'
						)
					)
				),
			}
		);
	}

	// return compoonent definition
	return {
		viewModel: ProcedureOccurrenceViewModel,
		template: template,
	};
});

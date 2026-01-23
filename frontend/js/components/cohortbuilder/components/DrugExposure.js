define([
	'knockout',
	'../options',
	'../utils',
	'../InputTypes/Range',
	'../InputTypes/DateAdjustment',
	'../InputTypes/Text',
	'../CriteriaGroup',
	'text!./DrugExposureTemplate.html',
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
	function DrugExposureViewModel(params) {
		var self = this;
		self.formatOption = function (d) {
			return (
				'<div class="optionText">' +
				d.text +
				'</div>' +
				'<div class="optionDescription">' +
				d.description +
				'</div>'
			);
		};

		self.addActions = [
			{
				...constants.drugexposureAttributes.addFirstDiagnosis,
				selected: false,
				action: function () {
					if (self.Criteria.First() == null)
						self.Criteria.First(true);
					else self.Criteria.First(null);
				},
				attributeName: 'First',
			},
			{
				...constants.drugexposureAttributes.addAge,
				selected: false,
				action: function () {
					if (self.Criteria.Age() == null)
						self.Criteria.Age(new Range());
					else self.Criteria.Age(null);
				},
				attributeName: 'Age',
			},
			{
				...constants.drugexposureAttributes.addGender,
				selected: false,
				action: function () {
					if (self.Criteria.Gender() == null)
						self.Criteria.Gender(ko.observableArray());
					else self.Criteria.Gender(null);
				},
				attributeName: 'Gender',
			},
			{
				...constants.drugexposureAttributes.addStartDate,
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
				...constants.drugexposureAttributes.addEndDate,
				selected: false,
				action: function () {
					if (self.Criteria.OccurrenceEndDate() == null)
						self.Criteria.OccurrenceEndDate(
							new Range({
								Op: 'lt',
							})
						);
					else self.Criteria.OccurrenceEndDate(null);
				},
				attributeName: 'OccurrenceEndDate',
			},
			{
				...constants.drugexposureAttributes.addDateAdjustment,
				selected: false,
				action: function () {
					if (self.Criteria.DateAdjustment() == null)
						self.Criteria.DateAdjustment(new DateAdjustment());
					else self.Criteria.DateAdjustment(null);
				},
				attributeName: 'DateAdjustment',
			},
			{
				...constants.drugexposureAttributes.addType,
				selected: false,
				action: function () {
					if (self.Criteria.DrugType() == null)
						self.Criteria.DrugType(ko.observableArray());
					else self.Criteria.DrugType(null);
				},
				attributeName: 'DrugType',
			},
			{
				...constants.drugexposureAttributes.addVisit,
				selected: false,
				action: function () {
					if (self.Criteria.VisitType() == null)
						self.Criteria.VisitType(ko.observableArray());
					else self.Criteria.VisitType(null);
				},
				attributeName: 'VisitType',
			},
			{
				...constants.drugexposureAttributes.addStopReason,
				selected: false,
				action: function () {
					if (self.Criteria.StopReason() == null)
						self.Criteria.StopReason(
							new Text({
								Op: 'contains',
							})
						);
					else self.Criteria.StopReason(null);
				},
				attributeName: 'StopReason',
			},
			{
				...constants.drugexposureAttributes.addRefills,
				selected: false,
				action: function () {
					if (self.Criteria.Refills() == null)
						self.Criteria.Refills(
							new Range({
								Op: 'lt',
							})
						);
					else self.Criteria.Refills(null);
				},
				attributeName: 'Refills',
			},
			{
				...constants.drugexposureAttributes.addQuantity,
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
				...constants.drugexposureAttributes.addDaysSupply,
				selected: false,
				action: function () {
					if (self.Criteria.DaysSupply() == null)
						self.Criteria.DaysSupply(
							new Range({
								Op: 'lt',
							})
						);
					else self.Criteria.DaysSupply(null);
				},
				attributeName: 'DaysSupply',
			},
			{
				...constants.drugexposureAttributes.addRoute,
				selected: false,
				action: function () {
					if (self.Criteria.RouteConcept() == null)
						self.Criteria.RouteConcept(ko.observableArray());
					else self.Criteria.RouteConcept(null);
				},
				attributeName: 'RouteConcept',
			},
			/*
			{
				...constants.drugexposureAttributes.addEffective,
				selected: false,
				action: function () {
					if (self.Criteria.EffectiveDrugDose() == null)
						self.Criteria.EffectiveDrugDose(
							new Range({
								Op: 'lt',
							})
						);
					else self.Criteria.EffectiveDrugDose(null);
				},
				attributeName: 'EffectiveDrugDose',
			},
			*/
			{
				...constants.drugexposureAttributes.addUnit,
				selected: false,
				action: function () {
					if (self.Criteria.DoseUnit() == null)
						self.Criteria.DoseUnit(ko.observableArray());
					else self.Criteria.DoseUnit(null);
				},
				attributeName: 'DoseUnit',
			},
			{
				...constants.drugexposureAttributes.addLotNumber,
				selected: false,
				action: function () {
					if (self.Criteria.LotNumber() == null)
						self.Criteria.LotNumber(
							new Text({
								Op: 'contains',
							})
						);
					else self.Criteria.LotNumber(null);
				},
				attributeName: 'LotNumber',
			},
			{
				...constants.drugexposureAttributes.addSource,
				selected: false,
				action: function () {
					if (self.Criteria.DrugSourceConcept() == null)
						self.Criteria.DrugSourceConcept(ko.observable());
					else self.Criteria.DrugSourceConcept(null);
				},
				attributeName: 'DrugSourceConcept',
			},
			{
				...constants.drugexposureAttributes.addProviderSpecialty,
				selected: false,
				action: function () {
					if (self.Criteria.ProviderSpecialty() == null)
						self.Criteria.ProviderSpecialty(ko.observableArray());
					else self.Criteria.ProviderSpecialty(null);
				},
				attributeName: 'ProviderSpecialty',
			},
			{
				...constants.drugexposureAttributes.addNested,
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
		self.Criteria = params.criteria.DrugExposure;
		self.options = options;
		self.hideClose = params.hideClose;

		self.removeCriteria = function () {
			params.deleteCallback();
		};

		self.removeCriterion = function (propertyName) {
			self.Criteria[propertyName](null);
		};
		self.indexMessage = ko.i18nformat(
			'components.conditionDrugExposure.indexDataText',
			'The index date refers to the drug exposure of <%= conceptSetName %>.',
			{
				conceptSetName: ko.pureComputed(() =>
					utils.getConceptSetName(
						self.Criteria.CodesetId,
						self.expression.ConceptSets,
						ko.i18n(
							'components.conditionDrugExposure.anyDrug',
							'Any Drug'
						)
					)
				),
			}
		);
	}

	// return compoonent definition
	return {
		viewModel: DrugExposureViewModel,
		template: template,
	};
});

define([
	'knockout',
	'../options',
	'../utils',
	'../InputTypes/Range',
	'../InputTypes/DateAdjustment',
	'../InputTypes/Text',
	'../CriteriaGroup',
	'text!./SpecimenTemplate.html',
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
	function SpecimenViewModel(params) {
		var self = this;
		self.expression = ko.utils.unwrapObservable(params.expression);
		self.Criteria = params.criteria.Specimen;
		self.options = options;
		self.hideClose = params.hideClose;

		self.removeCriteria = function () {
			params.deleteCallback();
		};

		self.addActions = [
			{
				...constants.specimenAttributes.addFirstDiagnosis,
				selected: false,
				action: function () {
					if (self.Criteria.First() == null)
						self.Criteria.First(true);
					else self.Criteria.First(null);
				},
				attributeName: 'First',
			},
			{
				...constants.specimenAttributes.addAge,
				selected: false,
				action: function () {
					if (self.Criteria.Age() == null)
						self.Criteria.Age(new Range());
					else self.Criteria.Age(null);
				},
				attributeName: 'Age',
			},
			{
				...constants.specimenAttributes.addGender,
				selected: false,
				action: function () {
					if (self.Criteria.Gender() == null)
						self.Criteria.Gender(ko.observableArray());
					else self.Criteria.Gender(null);
				},
				attributeName: 'Gender',
			},
			{
				...constants.specimenAttributes.addDate,
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
				...constants.specimenAttributes.addDateAdjustment,
				selected: false,
				action: function () {
					if (self.Criteria.DateAdjustment() == null)
						self.Criteria.DateAdjustment(new DateAdjustment());
					else self.Criteria.DateAdjustment(null);
				},
				attributeName: 'DateAdjustment',
			},
			{
				...constants.specimenAttributes.addType,
				selected: false,
				action: function () {
					if (self.Criteria.SpecimenType() == null)
						self.Criteria.SpecimenType(ko.observableArray());
					else self.Criteria.SpecimenType(null);
				},
				attributeName: 'SpecimenType',
			},
			{
				...constants.specimenAttributes.addQuantity,
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
				...constants.specimenAttributes.addUnit,
				selected: false,
				action: function () {
					if (self.Criteria.Unit() == null)
						self.Criteria.Unit(ko.observableArray());
					else self.Criteria.Unit(null);
				},
				attributeName: 'Unit',
			},
			{
				...constants.specimenAttributes.addAnatomicSite,
				selected: false,
				action: function () {
					if (self.Criteria.AnatomicSite() == null)
						self.Criteria.AnatomicSite(ko.observableArray());
					else self.Criteria.AnatomicSite(null);
				},
				attributeName: 'AnatomicSite',
			},
			{
				...constants.specimenAttributes.addDiseaseStatus,
				selected: false,
				action: function () {
					if (self.Criteria.DiseaseStatus() == null)
						self.Criteria.DiseaseStatus(ko.observableArray());
					else self.Criteria.DiseaseStatus(null);
				},
				attributeName: 'DiseaseStatus',
			},
			{
				...constants.specimenAttributes.addSourceId,
				selected: false,
				action: function () {
					if (self.Criteria.SourceId() == null)
						self.Criteria.SourceId(
							new Text({
								Op: 'contains',
							})
						);
					else self.Criteria.SourceId(null);
				},
				attributeName: 'SourceId',
			},
			{
				...constants.specimenAttributes.addNested,
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
			'components.conditionSpecimen.indexDataText',
			'The index date refers to the specimen of <%= conceptSetName %>.',
			{
				conceptSetName: ko.pureComputed(() =>
					utils.getConceptSetName(
						self.Criteria.CodesetId,
						self.expression.ConceptSets,
						ko.i18n(
							'components.conditionSpecimen.anySpecimen',
							'Any Specimen'
						)
					)
				),
			}
		);
	}

	// return compoonent definition
	return {
		viewModel: SpecimenViewModel,
		template: template,
	};
});

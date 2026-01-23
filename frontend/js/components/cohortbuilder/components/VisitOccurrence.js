define([
	'knockout',
	'appConfig',
	'../options',
	'../utils',
	'../InputTypes/Range',
	'../InputTypes/DateAdjustment',
	'../CriteriaGroup',
	'text!./VisitOccurrenceTemplate.html',
	'../const',
], function (
	ko,
	config,
	options,
	utils,
	Range,
	DateAdjustment,
	CriteriaGroup,
	template,
	constants
) {
	function VisitOccurrenceViewModel(params) {
		var self = this;
		self.addActions = [
			{
				...constants.visitAttributes.addFirstDiagnosis,
				selected: false,
				action: function () {
					if (self.Criteria.First() == null)
						self.Criteria.First(true);
					else self.Criteria.First(null);
				},
				attributeName: 'First',
			},
			{
				...constants.visitAttributes.addAge,
				selected: false,
				action: function () {
					if (self.Criteria.Age() == null)
						self.Criteria.Age(new Range());
					else self.Criteria.Age(null);
				},
				attributeName: 'Age',
			},
			{
				...constants.visitAttributes.addGender,
				selected: false,
				action: function () {
					if (self.Criteria.Gender() == null)
						self.Criteria.Gender(ko.observableArray());
					else self.Criteria.Gender(null);
				},
				attributeName: 'Gender',
			},
			{
				...constants.visitAttributes.addStartDate,
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
				...constants.visitAttributes.addEndDate,
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
				...constants.visitAttributes.addDateAdjustment,
				selected: false,
				action: function () {
					if (self.Criteria.DateAdjustment() == null)
						self.Criteria.DateAdjustment(new DateAdjustment());
					else self.Criteria.DateAdjustment(null);
				},
				attributeName: 'DateAdjustment',
			},
			{
				...constants.visitAttributes.addType,
				selected: false,
				action: function () {
					if (self.Criteria.VisitType() == null)
						self.Criteria.VisitType(ko.observableArray());
					else self.Criteria.VisitType(null);
				},
				attributeName: 'VisitType',
			},
			{
				...constants.visitAttributes.addLength,
				selected: false,
				action: function () {
					if (self.Criteria.VisitLength() == null)
						self.Criteria.VisitLength(new Range());
					else self.Criteria.VisitLength(null);
				},
				attributeName: 'VisitLength',
			},
			{
				...constants.visitAttributes.addSourceConcept,
				selected: false,
				action: function () {
					if (self.Criteria.VisitSourceConcept() == null)
						self.Criteria.VisitSourceConcept(ko.observable());
					else self.Criteria.VisitSourceConcept(null);
				},
				attributeName: 'VisitSourceConcept',
			},
			{
				...constants.visitAttributes.addProviderSpecialty,
				selected: false,
				action: function () {
					if (self.Criteria.ProviderSpecialty() == null)
						self.Criteria.ProviderSpecialty(ko.observableArray());
					else self.Criteria.ProviderSpecialty(null);
				},
				attributeName: 'ProviderSpecialty',
			},
			{
				...constants.visitAttributes.addPlaceService,
				selected: false,
				action: function () {
					if (self.Criteria.PlaceOfService() == null)
						self.Criteria.PlaceOfService(ko.observableArray());
					else self.Criteria.PlaceOfService(null);
				},
				attributeName: 'PlaceOfService',
			},
			{
				...constants.visitAttributes.addPlaceServiceLocation,
				selected: false,
				action: function () {
					if (self.Criteria.PlaceOfServiceLocation() == null) {
						self.Criteria.PlaceOfServiceLocation(ko.observable());
					}
					else self.Criteria.PlaceOfServiceLocation(null);
				},
				attributeName: 'PlaceOfServiceLocation',
			},
			{
				...constants.visitAttributes.addNested,
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

		if (config.features.locationDistance) {
			self.addActions.splice(self.addActions.length - 1, 0, {
				...constants.visitAttributes.addPlaceServiceDistance,
				selected: false,
				action: function () {
					if (self.Criteria.PlaceOfServiceDistance() == null) {
						self.Criteria.PlaceOfServiceDistance(new Range());
					}
				},
			});
		}

		self.expression = ko.utils.unwrapObservable(params.expression);
		self.Criteria = params.criteria.VisitOccurrence;
		self.options = options;
		self.hideClose = params.hideClose;

		self.removeCriteria = function () {
			params.deleteCallback();
		};

		self.removeCriterion = function (propertyName) {
			self.Criteria[propertyName](null);
		};

		self.indexMessage = ko.i18nformat(
			'components.conditionVisit.indexDataText',
			'The index date refers to the visit of <%= conceptSetName %>.',
			{
				conceptSetName: ko.pureComputed(() =>
					utils.getConceptSetName(
						self.Criteria.CodesetId,
						self.expression.ConceptSets,
						ko.i18n(
							'components.conditionVisit.anyVisit',
							'Any Visit'
						)
					)
				),
			}
		);
	}

	// return compoonent definition
	return {
		viewModel: VisitOccurrenceViewModel,
		template: template,
	};
});

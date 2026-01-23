define([
	'knockout',
	'appConfig',
	'../options',
	'../utils',
	'../InputTypes/Range',
	'../InputTypes/DateAdjustment',
	'../InputTypes/ConceptSetSelection',
	'../CriteriaGroup',
	'text!./VisitDetailTemplate.html',
	'../const',
], function (
	ko,
	config,
	options,
	utils,
	Range,
	DateAdjustment,
	ConceptSetSelection,
	CriteriaGroup,
	template,
	constants
) {
	function VisitDetailViewModel(params) {
		var self = this;

		self.addActions = [
			{
				...constants.visitDetailAttributes.addFirstDiagnosis,
				selected: false,
				action: function () {
					if (self.Criteria.First() == null)
						self.Criteria.First(true);
					else self.Criteria.First(null);
				},
				attributeName: 'First',
			},
			{
				...constants.visitDetailAttributes.addAge,
				selected: false,
				action: function () {
					if (self.Criteria.Age() == null)
						self.Criteria.Age(new Range());
					else self.Criteria.Age(null);
				},
				attributeName: 'Age',
			},
			{
				...constants.visitDetailAttributes.addGender,
				selected: false,
				action: function () {
					if (self.Criteria.GenderCS() == null)
						self.Criteria.GenderCS(new ConceptSetSelection());
					else self.Criteria.GenderCS(null);
				},
				attributeName: 'GenderCS',
			},
			{
				...constants.visitDetailAttributes.addStartDate,
				selected: false,
				action: function () {
					if (self.Criteria.VisitDetailStartDate() == null)
						self.Criteria.VisitDetailStartDate(
							new Range({
								Op: 'lt',
							})
						);
					else self.Criteria.VisitDetailStartDate(null);
				},
				attributeName: 'VisitDetailStartDate',
			},
			{
				...constants.visitDetailAttributes.addEndDate,
				selected: false,
				action: function () {
					if (self.Criteria.VisitDetailEndDate() == null)
						self.Criteria.VisitDetailEndDate(
							new Range({
								Op: 'lt',
							})
						);
					else self.Criteria.VisitDetailEndDate(null);
				},
				attributeName: 'VisitDetailEndDate',
			},
			{
				...constants.visitDetailAttributes.addDateAdjustment,
				selected: false,
				action: function () {
					if (self.Criteria.DateAdjustment() == null)
						self.Criteria.DateAdjustment(new DateAdjustment());
					else self.Criteria.DateAdjustment(null);
				},
				attributeName: 'DateAdjustment',
			},
			{
				...constants.visitDetailAttributes.addType,
				selected: false,
				action: function () {
					if (self.Criteria.VisitDetailTypeCS() == null)
						self.Criteria.VisitDetailTypeCS(
							new ConceptSetSelection()
						);
					else self.Criteria.VisitDetailTypeCS(null);
				},
				attributeName: 'VisitDetailTypeCS',
			},
			{
				...constants.visitDetailAttributes.addLength,
				selected: false,
				action: function () {
					if (self.Criteria.VisitDetailLength() == null)
						self.Criteria.VisitDetailLength(new Range());
					else self.Criteria.VisitDetailLength(null);
				},
				attributeName: 'VisitDetailLength',
			},
			{
				...constants.visitDetailAttributes.addSourceConcept,
				selected: false,
				action: function () {
					if (self.Criteria.VisitDetailSourceConcept() == null)
						self.Criteria.VisitDetailSourceConcept(ko.observable());
					else self.Criteria.VisitDetailSourceConcept(null);
				},
				attributeName: 'VisitDetailSourceConcept',
			},
			{
				...constants.visitDetailAttributes.addProviderSpecialty,
				selected: false,
				action: function () {
					if (self.Criteria.ProviderSpecialtyCS() == null)
						self.Criteria.ProviderSpecialtyCS(
							new ConceptSetSelection()
						);
					else self.Criteria.ProviderSpecialtyCS(null);
				},
				attributeName: 'ProviderSpecialtyCS',
			},
			{
				...constants.visitDetailAttributes.addPlaceService,
				selected: false,
				action: function () {
					if (self.Criteria.PlaceOfServiceCS() == null)
						self.Criteria.PlaceOfServiceCS(
							new ConceptSetSelection()
						);
					else self.Criteria.PlaceOfServiceCS(null);
				},
				attributeName: 'PlaceOfServiceCS',
			},
			{
				...constants.visitDetailAttributes.addPlaceServiceLocation,
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
				...constants.visitDetailAttributes.addNested,
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
				...constants.visitDetailAttributes.addPlaceServiceDistance,
				selected: false,
				action: function () {
					if (self.Criteria.PlaceOfServiceDistance() == null) {
						self.Criteria.PlaceOfServiceDistance(new Range());
					}
				},
			});
		}

		self.expression = ko.utils.unwrapObservable(params.expression);
		self.Criteria = params.criteria.VisitDetail;
		self.options = options;
		self.hideClose = params.hideClose;

		self.removeCriteria = function () {
			params.deleteCallback();
		};

		self.removeCriterion = function (propertyName) {
			self.Criteria[propertyName](null);
		};

		self.indexMessage = ko.i18nformat(
			'components.conditionVisitDetail.indexDataText',
			'The index date refers to the visit detail of <%= conceptSetName %>.',
			{
				conceptSetName: ko.pureComputed(() =>
					utils.getConceptSetName(
						self.Criteria.CodesetId,
						self.expression.ConceptSets,
						ko.i18n(
							'components.conditionVisitDetail.anyVisitDetail',
							'Any Visit Detail'
						)
					)
				),
			}
		);
	}

	// return component definition
	return {
		viewModel: VisitDetailViewModel,
		template: template,
	};
});

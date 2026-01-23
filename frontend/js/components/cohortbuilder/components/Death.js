define([
	'knockout',
	'../options',
	'../utils',
	'../InputTypes/Range',
	'../InputTypes/DateAdjustment',
	'../CriteriaGroup',
	'text!./DeathTemplate.html',
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
	function DeathViewModel(params) {
		var self = this;

		self.addActions = [
			{
				...constants.deathAttributes.addAge,
				selected: false,
				action: function () {
					if (self.Criteria.Age() == null)
						self.Criteria.Age(new Range());
					else self.Criteria.Age(null);
				},
				attributeName: 'Age',
			},
			{
				...constants.deathAttributes.addGender,
				selected: false,
				action: function () {
					if (self.Criteria.Gender() == null)
						self.Criteria.Gender(ko.observableArray());
					else self.Criteria.Gender(null);
				},
				attributeName: 'Gender',
			},
			{
				...constants.deathAttributes.addDate,
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
				...constants.deathAttributes.addDateAdjustment,
				selected: false,
				action: function () {
					if (self.Criteria.DateAdjustment() == null)
						self.Criteria.DateAdjustment(new DateAdjustment());
					else self.Criteria.DateAdjustment(null);
				},
				attributeName: 'DateAdjustment',
			},
			{
				...constants.deathAttributes.addType,
				selected: false,
				action: function () {
					if (self.Criteria.DeathType() == null)
						self.Criteria.DeathType(ko.observableArray());
					else self.Criteria.DeathType(null);
				},
				attributeName: 'DeathType',
			},
			{
				...constants.deathAttributes.addSourceConcept,
				selected: false,
				action: function () {
					if (self.Criteria.DeathSourceConcept() == null)
						self.Criteria.DeathSourceConcept(ko.observable());
					else self.Criteria.DeathSourceConcept(null);
				},
				attributeName: 'DeathSourceConcept',
			},
			{
				...constants.deathAttributes.addNested,
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
		self.Criteria = params.criteria.Death;
		self.options = options;
		self.hideClose = params.hideClose;

		self.removeCriteria = function () {
			params.deleteCallback();
		};

		self.removeCriterion = function (propertyName) {
			self.Criteria[propertyName](null);
		};

		self.indexMessage = ko.i18nformat(
			'components.conditionDeath.indexDataText',
			'The index date refers to the death event of <%= conceptSetName %>.',
			{
				conceptSetName: ko.pureComputed(() =>
					utils.getConceptSetName(
						self.Criteria.CodesetId,
						self.expression.ConceptSets,
						ko.i18n(
							'components.conditionDeath.anyDeath',
							'Any Death'
						)
					)
				),
			}
		);
	}

	// return compoonent definition
	return {
		viewModel: DeathViewModel,
		template: template,
	};
});

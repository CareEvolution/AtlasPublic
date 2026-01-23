define([
	'knockout',
	'../options',
	'../utils',
	'../InputTypes/Range',
	'../InputTypes/DateAdjustment',
	'../CriteriaGroup',
	'text!./ConditionEraTemplate.html',
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
	function ConditionEraViewModel(params) {
		var self = this;
		self.expression = ko.utils.unwrapObservable(params.expression);
		self.Criteria = params.criteria.ConditionEra;
		self.options = options;
		self.hideClose = params.hideClose;

		self.removeCriteria = function () {
			params.deleteCallback();
		};

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
				...constants.eraAttributes.addFirstDiagnosis,
				selected: false,
				action: function () {
					if (self.Criteria.First() == null) {
						self.Criteria.First(true);
					} else {
						self.removeCriterion('First');
					}
				},
				attributeName: 'First',
			},
			{
				...constants.eraAttributes.addAgeAtStart,
				selected: false,
				action: function () {
					if (self.Criteria.AgeAtStart() == null)
						self.Criteria.AgeAtStart(new Range());
					else self.removeCriterion('AgeAtStart');
				},
				attributeName: 'AgeAtStart',
			},
			{
				...constants.eraAttributes.addAgeAtEnd,
				selected: false,
				action: function () {
					if (self.Criteria.AgeAtEnd() == null)
						self.Criteria.AgeAtEnd(new Range());
					else self.removeCriterion('AgeAtEnd');
				},
				attributeName: 'AgeAtEnd',
			},
			{
				...constants.eraAttributes.addGender,
				selected: false,
				action: function () {
					if (self.Criteria.Gender() == null)
						self.Criteria.Gender(ko.observableArray());
					else self.removeCriterion('Gender');
				},
				attributeName: 'Gender',
			},
			{
				...constants.eraAttributes.addStartDate,
				selected: false,
				action: function () {
					if (self.Criteria.EraStartDate() == null)
						self.Criteria.EraStartDate(
							new Range({
								Op: 'lt',
							})
						);
					else self.removeCriterion('EraStartDate');
				},
				attributeName: 'EraStartDate',
			},
			{
				...constants.eraAttributes.addEndDate,
				selected: false,
				action: function () {
					if (self.Criteria.EraEndDate() == null)
						self.Criteria.EraEndDate(
							new Range({
								Op: 'lt',
							})
						);
					else self.removeCriterion('EraEndDate');
				},
				attributeName: 'EraEndDate',
			},
			{
				...constants.eraAttributes.addDateAdjustment,
				selected: false,
				action: function () {
					if (self.Criteria.DateAdjustment() == null)
						self.Criteria.DateAdjustment(new DateAdjustment());
					else self.removeCriterion('DateAdjustment');
				},
				attributeName: 'DateAdjustment',
			},
			{
				...constants.eraAttributes.addConditonCount,
				selected: false,
				action: function () {
					if (self.Criteria.OccurrenceCount() == null)
						self.Criteria.OccurrenceCount(new Range());
					else self.removeCriterion('OccurrenceCount');
				},
				attributeName: 'OccurrenceCount',
			},
			{
				...constants.eraAttributes.addLength,
				selected: false,
				action: function () {
					if (self.Criteria.EraLength() == null)
						self.Criteria.EraLength(new Range());
					else self.removeCriterion('EraLength');
				},
				attributeName: 'EraLength',
			},
			{
				...constants.eraAttributes.addNested,
				selected: false,
				action: function () {
					if (self.Criteria.CorrelatedCriteria() == null)
						self.Criteria.CorrelatedCriteria(
							new CriteriaGroup(null, self.expression.ConceptSets)
						);
					else self.removeCriterion('CorrelatedCriteria');
				},
				attributeName: 'CorrelatedCriteria',
			},
		];

		self.removeCriterion = function (propertyName) {
			self.Criteria[propertyName](null);
		};

		self.indexMessage = ko.pureComputed(() => {
			var anyCondition = ko.i18n(
				'components.conditionEra.anyConditionButton',
				'Any Condition'
			);
			var message = ko.i18n(
				'components.conditionEra.returnText_1',
				'The index date refers to the condition era of'
			);
			var conceptSetName = utils.getConceptSetName(
				self.Criteria.CodesetId,
				self.expression.ConceptSets,
				anyCondition()
			);
			return `${message()} ${conceptSetName}.`;
		});
	}

	// return compoonent definition
	return {
		viewModel: ConditionEraViewModel,
		template: template,
	};
});

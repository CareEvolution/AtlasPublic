define([
	'knockout',
	'../options',
	'../InputTypes/Range',
	'../InputTypes/DateAdjustment',
	'../InputTypes/Period',
	'../CriteriaGroup',
	'text!./PayerPlanPeriodTemplate.html',
	'../const',
], function (
	ko,
	options,
	Range,
	DateAdjustment,
	Period,
	CriteriaGroup,
	template,
	constants
) {
	function PayerPlanPeriodViewModel(params) {
		var self = this;

		self.expression = ko.utils.unwrapObservable(params.expression);
		self.Criteria = params.criteria.PayerPlanPeriod;
		self.options = options;
		self.hideClose = params.hideClose;

		self.removeCriteria = function () {
			params.deleteCallback();
		};

		self.addActions = [
			{
				...constants.payerPlanAttributes.addFirstDiagnosis,
				selected: false,
				action: function () {
					if (self.Criteria.First() == null)
						self.Criteria.First(true);
					else self.Criteria.First(null);
				},
				attributeName: 'First',
			},
			{
				...constants.payerPlanAttributes.addAgeAtStart,
				selected: false,
				action: function () {
					if (self.Criteria.AgeAtStart() == null)
						self.Criteria.AgeAtStart(new Range());
					else self.Criteria.AgeAtStart(null);
				},
				attributeName: 'AgeAtStart',
			},
			{
				...constants.payerPlanAttributes.addAgeAtEnd,
				selected: false,
				action: function () {
					if (self.Criteria.AgeAtEnd() == null)
						self.Criteria.AgeAtEnd(new Range());
					else self.Criteria.AgeAtEnd(null);
				},
				attributeName: 'AgeAtEnd',
			},
			{
				...constants.payerPlanAttributes.addUserDefined,
				selected: false,
				action: function () {
					if (self.Criteria.UserDefinedPeriod() == null)
						self.Criteria.UserDefinedPeriod(new Period());
					else self.Criteria.UserDefinedPeriod(null);
				},
				attributeName: 'UserDefinedPeriod',
			},
			{
				...constants.payerPlanAttributes.addStartDate,
				selected: false,
				action: function () {
					if (self.Criteria.PeriodStartDate() == null)
						self.Criteria.PeriodStartDate(
							new Range({
								Op: 'lt',
							})
						);
					else self.Criteria.PeriodStartDate(null);
				},
				attributeName: 'PeriodStartDate',
			},
			{
				...constants.payerPlanAttributes.addEndDate,
				selected: false,
				action: function () {
					if (self.Criteria.PeriodEndDate() == null)
						self.Criteria.PeriodEndDate(
							new Range({
								Op: 'lt',
							})
						);
					else self.Criteria.PeriodEndDate(null);
				},
				attributeName: 'PeriodEndDate',
			},
			{
				...constants.payerPlanAttributes.addDateAdjustment,
				selected: false,
				action: function () {
					if (self.Criteria.DateAdjustment() == null)
						self.Criteria.DateAdjustment(new DateAdjustment());
					else self.Criteria.DateAdjustment(null);
				},
				attributeName: 'DateAdjustment',
			},
			{
				...constants.payerPlanAttributes.addLength,
				selected: false,
				action: function () {
					if (self.Criteria.PeriodLength() == null)
						self.Criteria.PeriodLength(new Range());
					else self.Criteria.PeriodLength(null);
				},
				attributeName: 'PeriodLength',
			},
			{
				...constants.payerPlanAttributes.addGender,
				selected: false,
				action: function () {
					if (self.Criteria.Gender() == null)
						self.Criteria.Gender(ko.observableArray());
					else self.Criteria.Gender(null);
				},
				attributeName: 'Gender',
			},
			{
				...constants.payerPlanAttributes.addPayerConcept,
				selected: false,
				action: function () {
					if (self.Criteria.PayerConcept() == null)
						self.Criteria.PayerConcept(ko.observable());
					else self.Criteria.PayerConcept(null);
				},
				attributeName: 'PayerConcept',
			},
			{
				...constants.payerPlanAttributes.addPlanConcept,
				selected: false,
				action: function () {
					if (self.Criteria.PlanConcept() == null)
						self.Criteria.PlanConcept(ko.observable());
					else self.Criteria.PlanConcept(null);
				},
				attributeName: 'PlanConcept',
			},
			{
				...constants.payerPlanAttributes.addSponsorConcept,
				selected: false,
				action: function () {
					if (self.Criteria.SponsorConcept() == null)
						self.Criteria.SponsorConcept(ko.observable());
					else self.Criteria.SponsorConcept(null);
				},
				attributeName: 'SponsorConcept',
			},
			{
				...constants.payerPlanAttributes.addStopReasonConcept,
				selected: false,
				action: function () {
					if (self.Criteria.StopReasonConcept() == null)
						self.Criteria.StopReasonConcept(ko.observable());
					else self.Criteria.StopReasonConcept(null);
				},
				attributeName: 'StopReasonConcept',
			},
			{
				...constants.payerPlanAttributes.addPayerSourceConcept,
				selected: false,
				action: function () {
					if (self.Criteria.PayerSourceConcept() == null)
						self.Criteria.PayerSourceConcept(ko.observable());
					else self.Criteria.PayerSourceConcept(null);
				},
				attributeName: 'PayerSourceConcept',
			},
			{
				...constants.payerPlanAttributes.addPlanSourceConcept,
				selected: false,
				action: function () {
					if (self.Criteria.PlanSourceConcept() == null)
						self.Criteria.PlanSourceConcept(ko.observable());
					else self.Criteria.PlanSourceConcept(null);
				},
				attributeName: 'PlanSourceConcept',
			},
			{
				...constants.payerPlanAttributes.addSponsorSourceConcept,
				selected: false,
				action: function () {
					if (self.Criteria.SponsorSourceConcept() == null)
						self.Criteria.SponsorSourceConcept(ko.observable());
					else self.Criteria.SponsorSourceConcept(null);
				},
				attributeName: 'SponsorSourceConcept',
			},
			{
				...constants.payerPlanAttributes.addStopReasonSourceConcept,
				selected: false,
				action: function () {
					if (self.Criteria.StopReasonSourceConcept() == null)
						self.Criteria.StopReasonSourceConcept(ko.observable());
					else self.Criteria.StopReasonSourceConcept(null);
				},
				attributeName: 'StopReasonSourceConcept',
			},
			{
				...constants.payerPlanAttributes.addNested,
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

		self.indexMessage = ko.i18n(
			'components.conditionPayerPlanPeriod.indexDataText',
			'The index date refers to the payer plan period.'
		);
	}

	return {
		viewModel: PayerPlanPeriodViewModel,
		template: template,
	};
});

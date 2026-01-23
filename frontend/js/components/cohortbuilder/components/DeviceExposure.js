define([
	'knockout',
	'../options',
	'../utils',
	'../InputTypes/Range',
	'../InputTypes/Text',
	'../InputTypes/DateAdjustment',
	'../CriteriaGroup',
	'text!./DeviceExposureTemplate.html',
	'../const',
], function (
	ko,
	options,
	utils,
	Range,
	Text,
	DateAdjustment,
	CriteriaGroup,
	template,
	constants
) {
	function DeviceExposureViewModel(params) {
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
				...constants.deviceAttributes.addFirstDiagnosis,
				selected: false,
				action: function () {
					if (self.Criteria.First() == null)
						self.Criteria.First(true);
					else self.Criteria.First(null);
				},
				attributeName: 'First',
			},
			{
				...constants.deviceAttributes.addAge,
				selected: false,
				action: function () {
					if (self.Criteria.Age() == null)
						self.Criteria.Age(new Range());
					else self.Criteria.Age(null);
				},
				attributeName: 'Age',
			},
			{
				...constants.deviceAttributes.addGender,
				selected: false,
				action: function () {
					if (self.Criteria.Gender() == null)
						self.Criteria.Gender(ko.observableArray());
					else self.Criteria.Gender(null);
				},
				attributeName: 'Gender',
			},
			{
				...constants.deviceAttributes.addStartDate,
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
				...constants.deviceAttributes.addEndDate,
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
				...constants.deviceAttributes.addDateAdjustment,
				selected: false,
				action: function () {
					if (self.Criteria.DateAdjustment() == null)
						self.Criteria.DateAdjustment(new DateAdjustment());
					else self.Criteria.DateAdjustment(null);
				},
				attributeName: 'DateAdjustment',
			},
			/*
			{
				...constants.deviceAttributes.addType,
				selected: false,
				action: function () {
					if (self.Criteria.DeviceType() == null)
						self.Criteria.DeviceType(ko.observableArray());
					else self.Criteria.DeviceType(null);
				},
				attributeName: 'DeviceType',
			},
			*/
			{
				...constants.deviceAttributes.addVisit,
				selected: false,
				action: function () {
					if (self.Criteria.VisitType() == null)
						self.Criteria.VisitType(ko.observableArray());
					else self.Criteria.VisitType(null);
				},
				attributeName: 'VisitType',
			},
			{
				...constants.deviceAttributes.addUniqueId,
				selected: false,
				action: function () {
					if (self.Criteria.UniqueDeviceId() == null)
						self.Criteria.UniqueDeviceId(
							new Text({
								Op: 'contains',
							})
						);
					else self.Criteria.UniqueDeviceId(null);
				},
				attributeName: 'UniqueDeviceId',
			},
			{
				...constants.deviceAttributes.addQuantity,
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
				...constants.deviceAttributes.addSourceConcept,
				selected: false,
				action: function () {
					if (self.Criteria.DeviceSourceConcept() == null)
						self.Criteria.DeviceSourceConcept(ko.observable());
					else self.Criteria.DeviceSourceConcept(null);
				},
				attributeName: 'DeviceSourceConcept',
			},
			{
				...constants.deviceAttributes.addProviderSpecialty,
				selected: false,
				action: function () {
					if (self.Criteria.ProviderSpecialty() == null)
						self.Criteria.ProviderSpecialty(ko.observableArray());
					else self.Criteria.ProviderSpecialty(null);
				},
				attributeName: 'ProviderSpecialty',
			},
			{
				...constants.deviceAttributes.addNested,
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
		self.Criteria = params.criteria.DeviceExposure;
		self.options = options;
		self.hideClose = params.hideClose;

		self.removeCriteria = function () {
			params.deleteCallback();
		};

		self.removeCriterion = function (propertyName) {
			self.Criteria[propertyName](null);
		};

		self.indexMessage = ko.i18nformat(
			'components.conditionDevice.indexDataText',
			'The index date refers to the device exposure of <%= conceptSetName %>.',
			{
				conceptSetName: ko.pureComputed(() =>
					utils.getConceptSetName(
						self.Criteria.CodesetId,
						self.expression.ConceptSets,
						ko.i18n(
							'components.conditionDevice.anyDevice',
							'Any Device'
						)
					)
				),
			}
		);
	}

	// return compoonent definition
	return {
		viewModel: DeviceExposureViewModel,
		template: template,
	};
});

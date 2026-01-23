define([
	'knockout',
	'utils/AutoBind',
	'components/Component',
	'utils/CommonUtils',
	'atlas-state',
	'services/AuthAPI',
	'const',
	'services/CohortDefinition',
	'services/JobDetailsService',
	'appConfig',
	'text!./cohort-generator.html',
	'./reporting/cohort-reports/cohort-reports',
], function (
	ko,
	AutoBind,
	Component,
	commonUtils,
	sharedState,
	authApi,
	globalConstants,
	cohortDefinitionService,
	jobDetailsService,
	config,
	view
) {
	class CohortGenerator extends AutoBind(Component) {
		constructor(params) {
			super(params);

			this.currentCohortDefinition = params.cohortDefinition;
			this.isNew = params.isNew;
			this.criticalCount = params.criticalCount;

			this.showOnlySourcesWithResults = ko.observable(false);

			this.cohortDefinitionSourceInfo =
				sharedState.CohortDefinition.sourceInfo;
			this.dirtyFlag = sharedState.CohortDefinition.dirtyFlag;

			this.sourcesTableOptions = commonUtils.getTableOptions('S');
			this.sourcesColumns = [
				{
					title: `<span>${ko.i18n(
						'cohortDefinitions.cohortDefinitionManager.panels.sourceName',
						'Source Name'
					)()}</span>`,
					data: 'name',
				},
				{
					title: ko.i18n(
						'cohortDefinitions.cohortDefinitionManager.panels.generationStatus',
						'Generation Status'
					),
					render: () =>
						`<span data-bind="text: $data.status"></span>`,
				},
				{
					title: ko.i18n(
						'cohortDefinitions.cohortDefinitionManager.panels.people',
						'People'
					),
					render: () =>
						`<span data-bind="text: $data.personCount"></span>`,
				},
				{
					title: ko.i18n(
						'cohortDefinitions.cohortDefinitionManager.panels.records',
						'Records'
					),
					render: () =>
						`<span data-bind="text: $data.recordCount"></span>`,
				},
				{
					title: ko.i18n(
						'cohortDefinitions.cohortDefinitionManager.panels.generated',
						'Generated'
					),
					render: () =>
						`<span data-bind="text: $data.startTime"></span>`,
				},
				{
					title: ko.i18n(
						'cohortDefinitions.cohortDefinitionManager.panels.generationDuration',
						'Generation Duration'
					),
					render: () =>
						`<span data-bind="text: $data.executionDuration"></span>`,
				},
				{
					sortable: false,
					className: 'generation-buttons-column',
					render: () =>
						`<span data-bind="template: { name: 'generation-buttons', data: $data }"></span>`,
				},
			];

			this.stopping = ko.pureComputed(() =>
				this.cohortDefinitionSourceInfo().reduce(
					(acc, target) => ({
						...acc,
						[target.sourceKey]: ko.observable(false),
					}),
					{}
				)
			);

			this.selectedReportSource = ko.observable();
		}

		hasAccessToGenerate = (sourceKey) => {
			if (this.isNew()) {
				return false;
			}
			return this.currentCohortDefinition().hasWriteAccess();
		};

		hasInitialEvent = () => {
			return (
				this.currentCohortDefinition() &&
				this.currentCohortDefinition()
					.expression()
					.PrimaryCriteria()
					.CriteriaList().length > 0
			);
		};

		canGenerate = ko.pureComputed(() => {
			const isDirty = this.dirtyFlag() && this.dirtyFlag().isDirty();
			const isNew = this.isNew();
			const hasInitialEvent = this.hasInitialEvent();
			const isValid = this.criticalCount() <= 0;
			const canGenerate =
				!(isDirty || isNew) && hasInitialEvent && isValid;
			return canGenerate;
		});

		generateDisabledReason = ko.pureComputed(() => {
			if (this.criticalCount() > 0)
				return ko.unwrap(
					globalConstants.disabledReasons.INVALID_DESIGN
				);
			if (!this.hasInitialEvent())
				return ko.unwrap(
					globalConstants.disabledReasons.EMPTY_INITIAL_EVENT
				);
			if (this.dirtyFlag().isDirty())
				return ko.unwrap(globalConstants.disabledReasons.DIRTY);
			return null;
		});

		isSourceRunning(source) {
			if (source) {
				switch (source.status()) {
					case 'COMPLETE':
						return false;
					case 'n/a':
						return false;
					default:
						return true;
				}
			} else {
				return false;
			}
		}

		getSourceKeyInfo(sourceKey) {
			return this.cohortDefinitionSourceInfo().find((d) => {
				return d.sourceKey == sourceKey;
			});
		}

		generateCohort(source) {
			this.stopping()[source.sourceKey](false);
			this.getSourceKeyInfo(source.sourceKey).status(
				globalConstants.generationStatuses.PENDING
			);
			this.getSourceKeyInfo(source.sourceKey).createdBy(
				authApi.subject()
			);
			if (
				this.selectedReportSource() &&
				this.selectedReportSource().sourceId === source.sourceId
			) {
				this.toggleCohortReport(source);
			}
			cohortDefinitionService
				.generate(this.currentCohortDefinition().id(), source.sourceKey)
				.catch(authApi.handleAccessDenied)
				.then(({ data }) => {
					jobDetailsService.createJob(data);
				});
		}

		isSourceStopping = (source) => this.stopping()[source.sourceKey];

		toggleCohortReport(item) {
			if (
				this.selectedReportSource() &&
				this.selectedReportSource().sourceKey === item.sourceKey
			) {
				this.selectedReportSource(null);
				commonUtils.routeTo(
					'/cohortdefinition/' +
						this.currentCohortDefinition().id() +
						'/generation'
				);
			} else {
				this.selectedReportSource(item);
				commonUtils.routeTo(
					'/cohortdefinition/' +
						this.currentCohortDefinition().id() +
						'/generation/' +
						item.sourceId
				);
			}
		}

		cancelGenerate(source) {
			this.stopping()[source.sourceKey](true);
			cohortDefinitionService.cancelGenerate(
				this.currentCohortDefinition().id(),
				source.sourceKey
			);
		}

		isCancelDisabled(source) {
			return (
				this.isSourceStopping(source)() ||
				(config.userAuthenticationEnabled
					? this.isProcessingByAnother(source)
					: false)
			);
		}

		isProcessingByAnother(source) {
			return (
				!this.isMine(source) &&
				source.status() !== 'COMPLETE' &&
				source.status() !== 'FAILED'
			);
		}

		isMine(source) {
			return source.createdBy()
				? authApi.subject() === source.createdBy()
				: false;
		}
	}

	return commonUtils.build('cohort-generator', CohortGenerator, view);
});

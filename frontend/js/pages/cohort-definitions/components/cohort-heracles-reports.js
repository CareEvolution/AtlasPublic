define([
	'knockout',
	'utils/AutoBind',
	'components/Component',
	'appConfig',
	'utils/CommonUtils',
	'atlas-state',
	'services/CohortReporting',
	'pages/cohort-definitions/const',
	'services/Poll',
	'services/JobPollService',
	'services/Jobs',
	'services/JobDetailsService',
	'services/MomentAPI',
	'services/CohortDefinition',
	'lodash',
	'text!./cohort-heracles-reports.html',
	'./reporting/cost-utilization/report-manager',
	'components/modal-pick-options',
], function (
	ko,
	AutoBind,
	Component,
	config,
	commonUtils,
	sharedState,
	cohortReportingService,
	costUtilConst,
	{ PollService },
	JobPollService,
	jobService,
	jobDetailsService,
	momentApi,
	cohortDefinitionService,
	_,
	view
) {
	class CohortHeraclesReports extends AutoBind(Component) {
		constructor(params) {
			super(params);

			this.currentCohortDefinition = params.cohortDefinition;
			this.cdmSources = params.cdmSources;

			this.cohortDefinitionSourceInfo =
				sharedState.CohortDefinition.sourceInfo;

			// Reports

			this.sourceAnalysesStatus = {};
			this.cdmSources().forEach((source) => {
				this.sourceAnalysesStatus[source.sourceKey] = ko.observable({
					ready: false,
					checking: false,
				});
			});

			this.reportSourceKey = ko.observable();
			this.reportCohortDefinitionId = ko.observable();
			this.reportReportName = ko.observable();
			this.loadingReport = ko.observable(false);
			this.reportTriggerRun = ko.observable(false);
			this.isReportGenerating = ko.observable(false);

			// reporting sub-system
			this.generateButtonCaption = ko.observable('Generate Reports');
			this.generateReportsEnabled = ko.observable(false);
			this.createReportJobFailed = ko.observable(false);
			this.createReportJobError = ko.observable();
			this.reportingError = ko.observable();
			this.currentJob = ko.observable();
			this.reportingSourceStatusAvailable = ko.observable(false);
			this.reportingSourceStatusLoading = ko.observable(false);
			this.isGenerated = ko.observable(false);
			this.reportOptionCaption = ko.pureComputed(() => {
				return this.reportingSourceStatusLoading()
					? ko.i18n('common.loading', 'Loading Reports...')()
					: ko.i18n(
							'cohortDefinitions.cohortDefinitionManager.selectReport',
							'Select a Report'
					  )();
			});
			this.reportingSourceStatus = ko.observable();
			this.reportingAvailableReports = ko.observableArray();

			this.pollId = null;
			this.shouldUpdateJobs = ko.computed(() => {
				if (this.generateReportsEnabled()) {
					JobPollService.isJobListMutated(true);
				}
			});

			this.reportingState = ko.pureComputed(() => {
				// require a data source selection
				if (this.reportSourceKey() == undefined) {
					this.generateReportsEnabled(false);
					return 'awaiting_selection';
				}

				// check if the cohort has been generated
				var sourceInfo = this.cohortDefinitionSourceInfo().find(
					(d) => d.sourceKey == this.reportSourceKey()
				);
				if (this.getStatusMessage(sourceInfo) != 'COMPLETED') {
					this.generateReportsEnabled(false);
					return 'cohort_not_generated';
				}

				// check which reports have required data
				if (
					!this.reportingSourceStatusAvailable() &&
					!this.reportingSourceStatusLoading()
				) {
					this.reportingSourceStatusLoading(true);
					cohortReportingService
						.getCompletedAnalyses(
							sourceInfo,
							this.currentCohortDefinition().id()
						)
						.then((results) => {
							var reports =
								cohortReportingService.getAvailableReports(
									results
								);
							if (reports.length == 0) {
								this.reportingAvailableReports(reports);
								this.generateReportsEnabled(false);
								this.reportingSourceStatusAvailable(true);
								this.reportingSourceStatusLoading(false);
								return 'checking_status';
							}
							cohortReportingService
								.getCompletedHeraclesHeelAnalyses(
									sourceInfo,
									this.currentCohortDefinition().id()
								)
								.then((heelResults) => {
									if (heelResults.length > 0) {
										reports.push({
											name: ko.i18n(
												'cohortDefinitions.cohortDefinitionManager.heraclesHeel',
												'Heracles Heel'
											),
											reportKey: 'Heracles Heel',
											analyses: [],
										});
									}
									this.reportingAvailableReports(reports);
									this.generateReportsEnabled(false);
									this.reportingSourceStatusAvailable(true);
									this.reportingSourceStatusLoading(false);
									return 'checking_status';
								});
						});
				}

				// check if we can tell if the job to generate the reports is already running
				if (this.currentCohortDefinition() && this.currentJob()) {
					if (
						this.currentJob().status &&
						(this.currentJob().status == 'STARTED' ||
							this.currentJob().status == 'STARTING' ||
							this.currentJob().status == 'RUNNING')
					) {
						this.generateReportsEnabled(false);
						return 'generating_reports';
					}
				}

				if (this.reportingAvailableReports().length == 0) {
					// reset button to allow generation
					this.generateReportsEnabled(true);
					return 'reports_not_generated';
				}

				if (this.reportReportName() == undefined) {
					// reset button to allow regeneration
					this.generateReportsEnabled(true);
					return 'awaiting_selection';
				}

				if (this.currentCohortDefinition()) {
					this.generateReportsEnabled(true);
					this.reportCohortDefinitionId(
						this.currentCohortDefinition().id()
					);
					this.reportTriggerRun(true);
					return 'report_active';
				}

				var errorPackage = {};
				errorPackage.sourceAnalysesStatus =
					this.sourceAnalysesStatus[this.reportSourceKey()]();
				errorPackage.report = this.reportReportName();
				this.reportingError(JSON.stringify(errorPackage));
				// reset button to allow regeneration
				this.generateReportsEnabled(false);
				return 'unknown_cohort_report_state';
			});

			this.showReportNameDropdown = ko.pureComputed(() => {
				return (
					this.reportSourceKey() != undefined &&
					this.reportingState() != 'checking_status' &&
					this.reportingState() != 'cohort_not_generated' &&
					this.reportingState() != 'reports_not_generated' &&
					this.reportingState() != 'generating_reports'
				);
			});

			this.showUtilizationToRunModal = ko.observable(false);
			this.checkedUtilReports = ko.observableArray([]);

			const reportPacks = cohortReportingService.visualizationPacks;
			const reports = [
				reportPacks.healthcareUtilPersonAndExposureBaseline,
				reportPacks.healthcareUtilPersonAndExposureCohort,
				reportPacks.healthcareUtilVisitRecordsBaseline,
				reportPacks.healthcareUtilVisitRecordsCohort,
				reportPacks.healthcareUtilVisitDatesBaseline,
				reportPacks.healthcareUtilVisitDatesCohort,
				reportPacks.healthcareUtilCareSiteDatesBaseline,
				reportPacks.healthcareUtilCareSiteDatesCohort,
				reportPacks.healthcareUtilDrugBaseline,
				reportPacks.healthcareUtilDrugCohort,
			].map((item) => ({
				label: item.name,
				value: item.analyses,
			}));

			this.utilReportOptions = {
				reports: {
					title: ko.i18n('options.reports', 'Reports'),
					options: reports,
					selectedOptions: ko.observableArray([
						reportPacks.healthcareUtilPersonAndExposureCohort
							.analyses,
						reportPacks.healthcareUtilVisitRecordsCohort.analyses,
					]),
				},
				periods: {
					title: ko.i18n('options.periods', 'Periods'),
					options: costUtilConst.periods,
					selectedOptions: ko.observableArray([]),
				},
				rollups: {
					title: ko.i18n('options.rollups', 'Rollups'),
					options: costUtilConst.rollups,
					selectedOptions: ko.observableArray([]),
				},
			};

			this.subscriptions.push(
				this.reportSourceKey.subscribe((source) => {
					PollService.stop(this.pollId);
					this.reportReportName(null);
					this.reportingSourceStatusAvailable(false);
					this.reportingAvailableReports.removeAll();
					const cd = this.currentCohortDefinition();
					source && this.startPolling(cd, source);
				})
			);

			this.reportsManagerComponentParams = {
				reportSourceKey: this.reportSourceKey,
			};

			this.reportSourceKey(this.cdmSources()[0].sourceKey);
		}

		dispose() {
			super.dispose();
			PollService.stop(this.pollId);
		}

		startPolling(cd, source) {
			this.pollId = PollService.add({
				callback: () => this.queryHeraclesJob(cd, source),
				interval: config.pollInterval,
			});
		}

		async queryHeraclesJob(cd, source) {
			const testName = 'HERACLES_COHORT_' + cd.id() + '_' + source;
			try {
				const { data } = await jobService.getByName(
					testName,
					'cohortAnalysisJob'
				);
				if (data.jobParameters) {
					this.isGenerated(true);
					this.currentJob({
						...data,
						name: data.jobParameters.jobName,
						startDate: data.startDate
							? momentApi.formatDateTimeUTC(data.startDate)
							: '',
						duration: data.startDate
							? momentApi.formatDuration(
									(data.endDate || Date.now()) -
										data.startDate
							  )
							: '',
					});
				} else {
					this.currentJob(null);
					if (this.isGenerated()) {
						this.reportingSourceStatusAvailable(false);
					}
					this.isGenerated(false);
				}
			} catch (e) {
				console.error(e);
			}
		}

		async generateAnalyses({
			descr,
			duration,
			analysisIdentifiers,
			runHeraclesHeel,
			periods,
			rollupUtilizationVisit,
			rollupUtilizationDrug,
		}) {
			if (
				!confirm(
					ko.i18nformat(
						'cohortDefinitions.cohortDefinitionManager.confirms.generateAnalyses',
						'This will run <%=descr%> and may take about <%=duration%>. Are you sure?',
						{ descr: descr, duration: duration }
					)()
				)
			) {
				return;
			}

			this.generateReportsEnabled(false);
			analysisIdentifiers = _.uniq(analysisIdentifiers);
			var cohortDefinitionId = this.currentCohortDefinition().id();
			var cohortJob = {};

			cohortJob.jobName = `HERACLES_COHORT_${cohortDefinitionId}_${this.reportSourceKey()}`;
			cohortJob.sourceKey = this.reportSourceKey();
			cohortJob.smallCellCount = 5;
			cohortJob.cohortDefinitionIds = [];
			cohortJob.cohortDefinitionIds.push(cohortDefinitionId);
			cohortJob.analysisIds = analysisIdentifiers;
			cohortJob.runHeraclesHeel = runHeraclesHeel;
			cohortJob.cohortPeriodOnly = false;

			// set concepts
			cohortJob.conditionConceptIds = [];
			cohortJob.drugConceptIds = [];
			cohortJob.procedureConceptIds = [];
			cohortJob.observationConceptIds = [];
			cohortJob.measurementConceptIds = [];

			cohortJob.periods = periods;

			cohortJob.rollupUtilizationVisit = rollupUtilizationVisit;
			cohortJob.rollupUtilizationDrug = rollupUtilizationDrug;

			this.createReportJobFailed(false);
			try {
				this.isReportGenerating(true);
				const { data } =
					await cohortDefinitionService.getCohortAnalyses(cohortJob);
				jobDetailsService.createJob(data);
			} catch (err) {
				this.createReportJobFailed(true);
				const { status, data } = err;
				const createReportJobErrorPackage = {
					status,
					error: data.payload,
				};
				this.createReportJobError(
					JSON.stringify(createReportJobErrorPackage)
				);

				// reset button to allow generation attempt
				this.generateReportsEnabled(true);
				this.generateButtonCaption('Generate');
			}
			await this.queryHeraclesJob(
				this.currentCohortDefinition(),
				this.reportSourceKey()
			);
			this.isReportGenerating(false);
		}

		generateQuickAnalysis() {
			this.generateAnalyses({
				descr: ko.i18n(
					'cohortDefinitions.cohortDefinitionManager.generateQuickAnalysis.descr',
					'minimal analyses set to provide a quick overview of the cohort'
				)(),
				duration: ko.i18n(
					'cohortDefinitions.cohortDefinitionManager.generateQuickAnalysis.duration',
					'10 minutes'
				)(),
				analysisIdentifiers:
					cohortReportingService.getQuickAnalysisIdentifiers(),
				runHeraclesHeel: false,
			});
		}

		selectHealthcareAnalyses() {
			this.showUtilizationToRunModal(true);
		}

		generateHealthcareAnalyses() {
			const analysisIds = this.utilReportOptions.reports
				.selectedOptions()
				.reduce((acc, ids) => [...acc, ...ids], []);
			this.generateAnalyses({
				descr: ko.i18n(
					'cohortDefinitions.cohortDefinitionManager.generateHealthcareAnalyses.descr',
					'the Cost and Utilization analyses'
				)(),
				duration: ko.i18n(
					'cohortDefinitions.cohortDefinitionManager.generateHealthcareAnalyses.duration',
					'10-45 minutes'
				)(),
				analysisIdentifiers: analysisIds,
				runHeraclesHeel: false,
				periods: this.utilReportOptions.periods.selectedOptions(),
				...this.utilReportOptions.rollups
					.selectedOptions()
					.reduce((acc, current) => {
						acc[current] = true;
						return acc;
					}, {}),
			});

			this.showUtilizationToRunModal(false);
		}

		generateAllAnalyses() {
			this.generateAnalyses({
				descr: ko.i18n(
					'cohortDefinitions.cohortDefinitionManager.generateAllAnalyses.descr',
					'all analyses'
				)(),
				duration: ko.i18n(
					'cohortDefinitions.cohortDefinitionManager.generateAllAnalyses.duration',
					'60-90 minutes'
				)(),
				analysisIdentifiers:
					cohortReportingService.getAnalysisIdentifiers(),
				runHeraclesHeel: true,
				periods: costUtilConst.periods.map(p => p.value)
			});
		}

		getStatusMessage(info) {
			if (info.status() === 'COMPLETE' && !info.isValid())
				return !info.isCanceled() ? 'FAILED' : 'CANCELED';
			else
				return info.status() === 'COMPLETE'
					? 'COMPLETED'
					: info.status();
		}
	}

	return commonUtils.build(
		'cohort-heracles-reports',
		CohortHeraclesReports,
		view
	);
});

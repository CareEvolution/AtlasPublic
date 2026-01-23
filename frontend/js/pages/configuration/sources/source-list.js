define([
	'knockout',
	'components/Component',
	'text!./source-list.html',
	'utils/CommonUtils',
	'appConfig',
	'services/AuthAPI',
	'services/SourceAPI',
	'atlas-state',
	'const',
	'services/JobDetailsService',
	'services/Poll',
	'services/job/jobDetail',
	'services/CacheAPI',
	'less!./source-list.less',
	'components/heading',
], function (
	ko,
	Component,
	view,
	commonUtils,
	config,
	authApi,
	sourceApi,
	sharedState,
	constants,
	jobDetailsService,
	{ PollService },
	jobDetail,
	cacheApi
) {
	class SourceList extends Component {
		constructor(params) {
			super(params);
			this.config = config;
			this.loading = ko.observable(false);
			this.sharedState = sharedState;
			this.isInProgress = ko.observable(false);
			this.jobListing = sharedState.jobListing;
			this.sourceJobs = new Map();
			this.sources = sharedState.sources;

			this.canCreate = ko.observable(false);
			authApi.canCreateSource().then((result) => {
				this.canCreate(result);
			});

			this.priorityOptions = [
				{
					id: 'session',
					name: ko.i18n(
						'configuration.priorityOptions.session',
						'Current Session'
					),
				},
				{
					id: 'application',
					name: ko.i18n(
						'configuration.priorityOptions.application',
						'Whole Application'
					),
				},
			];

			this.initializationCompleted = ko.pureComputed(
				() =>
					sharedState.appInitializationStatus() ===
						constants.applicationStatuses.running ||
					sharedState.appInitializationStatus() ===
						constants.applicationStatuses.noSourcesAvailable
			);

			this.canCreateSource = ko.pureComputed(() => {
				if (!config.userAuthenticationEnabled || !this.isAuthenticated) {
					return false;
				}
				return this.canCreate();
			});

			this.canChangePriority = ko.pureComputed(() => {
				if (!config.userAuthenticationEnabled || !this.isAuthenticated()) {
					return false;
				}
				return this.sources().every((source) => source.hasWriteAccess);
			});

			this.canClearAllSourcesCache = ko.observable(false);
			authApi.canClearAllSourcesCache().then((result) => {
				this.canClearAllSourcesCache(result);
			});

			this.canClearServerCache = ko.pureComputed(() => {
				return (
					config.userAuthenticationEnabled &&
					this.isAuthenticated() &&
					this.canClearAllSourcesCache()
				);
			});

			this.intervalId = PollService.add({
				callback: () => this.checkJobs(),
				interval: config.pollInterval,
			});

			this.loading(true);
			sourceApi.initSourcesConfig().finally(() => {
				this.loading(false);
			});
		}

		dispose() {
			PollService.stop(this.intervalId);
		}

		getSource(job) {
			return this.sourceJobs.get(job.executionId);
		}

		async checkJobs() {
			const notifications =
				await jobDetailsService.listRefreshCacheJobs();
			const jobs = notifications.data.map((n) => {
				const job = new jobDetail();
				job.status(n.status);
				job.executionId = n.executionId;
				return job;
			});

			jobs.forEach((job) => {
				const source = this.getSource(job);
				if (source && (job.isComplete() || job.isFailed())) {
					this.sourceJobs.delete(job.executionId);
					source.refreshState(
						job.isComplete()
							? sourceApi.buttonCheckState.success
							: sourceApi.buttonCheckState.failed
					);
				}
			});
		}

		canReadSource(source) {
			if (!config.userAuthenticationEnabled || !this.isAuthenticated) {
				return false;
			}
			return true; // We only return sources that the user can read
		}

		canCheckConnection(source) {
			if (!config.userAuthenticationEnabled || !this.isAuthenticated()) {
				return false;
			}
			return true; // We only return sources that the user can read, and if they can read, they can check connection
		}

		canRefreshSourceCache(source) {
			if (!config.userAuthenticationEnabled || !this.isAuthenticated()) {
				return false;
			}
			return source.hasResults && (source.hasVocabulary || source.hasCDM) && source.hasWriteAccess;
		}

		canClearAnyCache() {
			return this.sources().some((source) => this.canRefreshSourceCache(source));
		}

		clearServerCache() {
			if (
				confirm(
					ko.unwrap(
						ko.i18n(
							'configuration.confirms.clearServerCache',
							'Are you sure you want to clear the server cache?'
						)
					)
				)
			) {
				cacheApi.clearCache().then(
					() => {
						alert(
							ko.unwrap(
								ko.i18n(
									'configuration.alerts.clearServerCache',
									'Server cache has been cleared.'
								)
							)
						);
					},
					(error) => {
						alert(
							ko.unwrap(
								ko.i18n(
									'configuration.alerts.clearServerCacheError',
									'There was an error! The server cache has NOT been cleared.'
								)
							)
						);
					}
				);
			}
		}

		newSource() {
			commonUtils.routeTo('/source/0');
		}

		selectSource(source) {
			document.location = '#/source/' + source.sourceId;
		}

		async updateSourceDaimonPriority(sourceKey, daimonType) {
			if (sharedState.priorityScope() !== 'application') {
				return;
			}
			this.isInProgress(true);
			try {
				await sourceApi.updateSourceDaimonPriority(
					sourceKey,
					daimonType
				);
				await sourceApi.initSourcesConfig();
			} catch (err) {
				alert(
					ko.unwrap(
						ko.i18n(
							'configuration.alerts.failUpdatePrioritySourceDaimon',
							'Failed to update priority source daimon'
						)
					)
				);
			}
			this.isInProgress(false);
		}

		updateVocabPriority() {
			var newVocabUrl = sharedState.vocabularyUrl();
			var selectedSource = sharedState.sources().find((item) => {
				return item.vocabularyUrl === newVocabUrl;
			});
			sharedState.priorityScope() === 'application' &&
				sharedState.defaultVocabularyUrl(newVocabUrl);
			this.updateSourceDaimonPriority(
				selectedSource.sourceKey,
				'Vocabulary'
			);
			return true;
		}

		updateEvidencePriority() {
			var newEvidenceUrl = sharedState.evidenceUrl();
			var selectedSource = sharedState.sources().find((item) => {
				return item.evidenceUrl === newEvidenceUrl;
			});
			sharedState.priorityScope() === 'application' &&
				sharedState.defaultEvidenceUrl(newEvidenceUrl);
			this.updateSourceDaimonPriority(selectedSource.sourceKey, 'CEM');
			return true;
		}

		updateResultsPriority() {
			var newResultsUrl = sharedState.resultsUrl();
			var selectedSource = sharedState.sources().find((item) => {
				return item.resultsUrl === newResultsUrl;
			});
			sharedState.priorityScope() === 'application' &&
				sharedState.defaultResultsUrl(newResultsUrl);
			this.updateSourceDaimonPriority(
				selectedSource.sourceKey,
				'Results'
			);
			return true;
		}

		checkSourceConnection(source) {
			sourceApi
				.checkSourceConnection(source.sourceKey)
				.then(({ data }) =>
					source.connectionCheck(
						data.sourceId === undefined
							? sourceApi.buttonCheckState.failed
							: sourceApi.buttonCheckState.success
					)
				)
				.catch(() => {
					source.connectionCheck(sourceApi.buttonCheckState.failed);
				});
			source.connectionCheck(sourceApi.buttonCheckState.checking);
		}

		async refreshSources() {
			try {
				this.loading(true);
				await sourceApi.refreshSources();
			} finally {
				this.loading(false);
			}
		}

		async clearSourceCache(source) {
			try {
				source.refreshState(sourceApi.buttonCheckState.checking);
				await sourceApi.clearSourceCache(source.sourceKey);
				source.refreshState(sourceApi.buttonCheckState.success);
			} catch (e) {
				console.log("Error clearing source cache:", e);
				source.refreshState(sourceApi.buttonCheckState.failed);
			}
		}

		getRefreshCacheButtonStyles(source) {
			return this.getButtonStyles(source.refreshState());
		}

		getCheckButtonStyles(source) {
			return this.getButtonStyles(source.connectionCheck());
		}

		getButtonStyles(sourceState) {
			let iconClass = 'fa-caret-right';
			let buttonClass = 'btn-outline-secondary';
			switch (sourceState) {
				case sourceApi.buttonCheckState.success:
					buttonClass = 'btn-success';
					iconClass = 'fa-check';
					break;
				case sourceApi.buttonCheckState.failed:
					buttonClass = 'btn-danger';
					iconClass = 'fa-exclamation';
					break;
				case sourceApi.buttonCheckState.checking:
					buttonClass = 'btn-warning';
					iconClass = 'fa-circle-o-notch fa-spin';
					break;
			}
			return {
				iconClass,
				buttonClass,
			};
		}
	}

	return commonUtils.build('source-list', SourceList, view);
});

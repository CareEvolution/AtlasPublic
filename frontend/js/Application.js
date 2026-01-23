define([
	'knockout',
	'services/http',
	'services/AuthAPI',
	'appConfig',
	'atlas-state',
	'jquery',
	'services/SourceAPI',
	'services/I18nService',
	'services/EventBus',
	'utils/BemHelper',
	'const',
	'bowser',
	'version',
	'less!app.less',
], (
	ko,
	httpService,
	authApi,
	config,
	sharedState,
	$,
	sourceApi,
	i18nService,
	EventBus,
	BemHelper,
	constants,
	bowser,
	versionInfo,
) => {
	return class Application {
		constructor(router) {
			// establish base priorities for daimons
			this.evidencePriority = 0;
			this.vocabularyPriority = 0;
			this.densityPriority = 0;
			this.router = router;
			this.EventBus = EventBus;
			const bemHelper = new BemHelper('app');
			this.classes = bemHelper.run.bind(bemHelper);
			this.hasUnsavedChanges = ko.pureComputed(() => {
				return (
					sharedState.RepositoryConceptSet.dirtyFlag().isDirty() ||
					sharedState.CohortDefinition.dirtyFlag().isDirty() ||
					sharedState.IRAnalysis.dirtyFlag().isDirty() ||
					sharedState.CohortPathways.dirtyFlag().isDirty() ||
					sharedState.estimationAnalysis.dirtyFlag().isDirty() ||
					sharedState.predictionAnalysis.dirtyFlag().isDirty() ||
					sharedState.CohortCharacterization.dirtyFlag().isDirty()
				);
			});
			this.initializationComplete = ko.pureComputed(() => {
				return (
					sharedState.appInitializationStatus() !=
					constants.applicationStatuses.initializing
				);
			});

			this.toggleBrowserWarning = function () {
				const browser = bowser.getParser(navigator.userAgent);
				const isValidBrowser = browser.satisfies({
					chrome: '>63',
					firefox: '>55',
					opera: '>47',
					edge: '>79',
					safari: '>11.1',
				});
				return !config.disableBrowserCheck && !isValidBrowser;
			};

			this.appInitializationStatus = sharedState.appInitializationStatus;
			this.noSourcesAvailable = ko.pureComputed(() => {
				return (
					this.appInitializationStatus() ===
						constants.applicationStatuses.noSourcesAvailable &&
					this.router.currentView() !== 'ohdsi-configuration'
				);
			});
			this.appInitializationErrorMessage = ko.computed(() => {
				if (this.noSourcesAvailable()) {
					return ko.i18n(
						'commonErrors.noSources',
						'The current WebAPI has no sources defined.<br/>Please add one or more on <a href="#/configure">configuration</a> page.'
					)();
				} else if (
					this.appInitializationStatus() !==
					constants.applicationStatuses.noSourcesAvailable
				) {
					return ko.i18n(
						'commonErrors.webapiConnectError',
						'Unable to connect to an instance of the WebAPI.<br/>Please contact your administrator to resolve this issue.'
					)();
				}
			});
			this.pageTitle = ko.pureComputed(() => {
				let pageTitle = 'ATLAS';
				switch (this.router.currentView()) {
					case 'loading':
						pageTitle =
							`${pageTitle}: ` +
							ko.i18n('common.loading', 'Loading')();
						break;
					default:
						pageTitle = `${pageTitle}: ${ko.unwrap(
							this.router.activeRoute().title
						)}`;
						break;
				}

				if (this.hasUnsavedChanges()) {
					pageTitle =
						`*${pageTitle} ` +
						ko.i18n('common.unsaved', '(unsaved)')();
				}

				return pageTitle;
			});
			this.companyInfoTemplate = config.companyInfoCustomHtmlTemplate;
			this.showCompanyInfo = config.showCompanyInfo;

			this.version = versionInfo.versionCommit === versionInfo.currentCommit ? versionInfo.version : `${versionInfo.version} (${versionInfo.currentCommit.substring(0, 7)})`;
		}

		/**
		 * Performs initial setup
		 * @returns Promise
		 */
		async bootstrap() {
			sharedState.appInitializationStatus(
				ko.observable(constants.applicationStatuses.initializing)
			);
			config.api.isExecutionEngineAvailable = ko.observable(false);
			ko.applyBindings(
				{
					// provide to a view access to both model and the router via this.router
					...this,
				},
				document.getElementsByTagName('html')[0]
			);
			httpService.setUnauthorizedHandler(() =>
				authApi.resetAuthParams()
			);

			await i18nService.getAvailableLocales();

			if (config.userAuthenticationEnabled) {
				if (!window.location.href.includes('/welcome/')) {
					await authApi.loadUserInfo();
				}
			}
			this.attachGlobalEventListeners();
		}

		/**
		 * Fetches all the data required for app's functioning
		 * @returns Promise
		 */
		synchronize() {
			const promise = Promise.all([
				this.initServiceInformation(),
			]);
			promise.then(() => {
				this.router.run();
			});

			return promise;
		}
		// service methods

		attachGlobalEventListeners() {
			const self = this;
			$(window).bind('beforeunload', function () {
				if (self.hasUnsavedChanges())
					return 'Changes will be lost if you do not save.';
			});
		}

		/**
		 * Initialize all service information asynchronously
		 * @returns Promise
		 */
		initServiceInformation() {
			console.info('Initializing service information');
			return new Promise((resolve, reject) => {
				sharedState.sources([]);

				if (
					config.userAuthenticationEnabled &&
					!authApi.isAuthenticated()
				) {
					this.authSubscription =
						authApi.isAuthenticated.subscribe(
							async (isAuthed) => {
								if (isAuthed) {
									sharedState.appInitializationStatus(
										await sourceApi.initSourcesConfig()
									);
									this.authSubscription.dispose();
									console.info(
										'Re-initialized service information'
									);
								}
							}
						);
					sharedState.appInitializationStatus(
						constants.applicationStatuses.running
					);
					resolve();
					return;
				} else {
					sourceApi
						.initSourcesConfig()
						.then(function (appStatus) {
							sharedState.appInitializationStatus(appStatus);
							console.info('Init sources from server');
							resolve();
						});
				}
			});
		}
		checkOAuthError() {
			const hash = window.location.hash;
			if (hash && hash.includes('oauth_error_email')) {
				alert(
					'Empty email received from oauth server. Check whether it has public access'
				);
			}
		}
	};
});

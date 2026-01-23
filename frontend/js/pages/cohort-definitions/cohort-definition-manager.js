define([
	'knockout',
	'utils/AutoBind',
	'text!./cohort-definition-manager.html',
	'appConfig',
	'components/cohortbuilder/CohortDefinition',
	'services/CohortDefinition',
	'services/MomentAPI',
	'services/Permission',
	'services/Tags',
	'components/conceptset/ConceptSetStore',
	'atlas-state',
	'd3',
	'pages/Page',
	'utils/CommonUtils',
	'services/AuthAPI',
	'services/Poll',
	'services/JobPollService',
	'services/file',
	'const',
	'./const',
	'components/security/access/const',
	'components/cohortbuilder/components/FeasibilityReportViewer',
	'databindings',
	'components/checks/warnings',
	'less!./cohort-definition-manager.css',
	'assets/ohdsi.util',
	'components/cohortbuilder/InclusionRule',
	'components/editable-heading',
	'components/modal',
	'components/modal-exit-message',
	'components/security/access/configure-access-modal',
	'components/tags/modal/tags-modal',
	'components/conceptset/conceptset-list',
	'components/name-validation',
	'components/versions/versions',
	'./components/cohort-definition-exporter',
	'./components/cohort-generator',
	'./components/cohort-samples',
	'./components/cohort-heracles-reports',
	'components/atlas.cohort-editor',
], function (
	ko,
	AutoBind,
	view,
	config,
	CohortDefinition,
	cohortDefinitionService,
	momentApi,
	PermissionService,
	TagsService,
	ConceptSetStore,
	sharedState,
	d3,
	Page,
	commonUtils,
	authApi,
	{ PollService },
	JobPollService,
	FileService,
	globalConstants,
	constants,
	{ entityType }
) {
	class CohortDefinitionManager extends AutoBind(Page) {
		constructor(params) {
			super(params);

			this.sharedState = sharedState;

			// Route parameters
			this.selectedSourceKey = ko.observable();
			this.selectedSampleId = ko.observable();

			this.previewVersion = sharedState.CohortDefinition.previewVersion;

			this.pollTimeoutId = null;
			this.authApi = authApi;
			this.config = config;
			this.enablePermissionManagement = config.enablePermissionManagement;
			this.commonUtils = commonUtils;
			this.isLoading = ko.observable(false);
			this.currentCohortDefinition = sharedState.CohortDefinition.current;
			this.currentCohortDefinitionMode =
				sharedState.CohortDefinition.mode;
			this.currentCohortDefinitionInfo =
				sharedState.CohortDefinition.info;
			this.cohortDefinitionSourceInfo =
				sharedState.CohortDefinition.sourceInfo;
			this.dirtyFlag = sharedState.CohortDefinition.dirtyFlag;
			this.conceptSets = ko.computed(
				() =>
					this.currentCohortDefinition() &&
					this.currentCohortDefinition().expression().ConceptSets
			);
			this.conceptSetStore = ConceptSetStore.getStore(
				ConceptSetStore.sourceKeys().cohortDefinition
			);

			this.tabMode = sharedState.CohortDefinition.mode;
			this.defaultName = ko.unwrap(
				globalConstants.newEntityNames.cohortDefinition
			);

			this.cdmSources = ko.pureComputed(() => {
				return sharedState
					.sources()
					.filter(
						(source) =>
							commonUtils.hasCDM(source)
					);
			});

			this.cohortDefinitionCaption = ko.pureComputed(() => {
				if (this.currentCohortDefinition()) {
					if (
						this.currentCohortDefinition().id() === 0 ||
						this.currentCohortDefinition().id() === null
					) {
						return this.defaultName;
					} else if (this.previewVersion()) {
						return ko.i18nformat(
							'cohortDefinitions.cohortCaptionPreview',
							'Cohort #<%=id%> - Version <%=number%> Preview',
							{
								id: this.currentCohortDefinition().id(),
								number: this.previewVersion().version,
							}
						)();
					} else {
						return ko.i18nformat(
							'cohortDefinitions.cohortCaption',
							'Cohort #<%=id%>',
							{ id: this.currentCohortDefinition().id() }
						)();
					}
				}
			});
			this.isNameFilled = ko.pureComputed(() => {
				return (
					this.currentCohortDefinition() &&
					this.currentCohortDefinition().name() &&
					this.currentCohortDefinition().name().trim()
				);
			});
			this.isNameCharactersValid = ko.pureComputed(() => {
				return (
					this.isNameFilled() &&
					commonUtils.isNameCharactersValid(
						this.currentCohortDefinition().name()
					)
				);
			});
			this.isNameLengthValid = ko.pureComputed(() => {
				return (
					this.isNameFilled() &&
					commonUtils.isNameLengthValid(
						this.currentCohortDefinition().name()
					)
				);
			});
			this.isDefaultName = ko.pureComputed(() => {
				return (
					this.isNameFilled() &&
					this.currentCohortDefinition().name().trim() ===
						this.defaultName
				);
			});

			this.isNameCorrect = ko.pureComputed(() => {
				return (
					this.isNameFilled() &&
					!this.isDefaultName() &&
					this.isNameCharactersValid() &&
					this.isNameLengthValid()
				);
			});
			this.isAuthenticated = ko.pureComputed(() => {
				return this.authApi.isAuthenticated();
			});
			this.isNew = ko.pureComputed(() => {
				return (
					!this.currentCohortDefinition() ||
					this.currentCohortDefinition().id() === 0
				);
			});
			this.isSaving = ko.observable(false);
			this.isCopying = ko.observable(false);
			this.isDeleting = ko.observable(false);
			this.isProcessing = ko.computed(() => {
				return this.isSaving() || this.isCopying() || this.isDeleting();
			});

			// Permissions
			this.canCreate = ko.observable(false);
			authApi.canCreateCohortDefinition().then((result) => {
				this.canCreate(result);
			});

			this.canEdit = ko.pureComputed(() => {
				if (!config.userAuthenticationEnabled) {
					return true;
				}

				if (!authApi.isAuthenticated()) {
					return false;
				}

				if (
					this.currentCohortDefinition() &&
					this.currentCohortDefinition().id() != 0
				) {
					return this.currentCohortDefinition().hasWriteAccess();
				} else {
					return this.canCreate();
				}
			});

			this.canCopy = ko.pureComputed(() => {
				return (
					!this.dirtyFlag().isDirty() &&
					!this.isNew() &&
					((this.isAuthenticated() &&
						this.canCreate()) ||
						!config.userAuthenticationEnabled)
				);
			});

			this.canDelete = ko.pureComputed(() => {
				if (this.isNew()) {
					return false;
				}
				if (!config.userAuthenticationEnabled) {
					return true;
				}
				if (!this.isAuthenticated()) {
					return false;
				}
				return this.currentCohortDefinition().hasDeleteAccess();
			});

			this.hasAccess = ko.pureComputed(() => {
				if (!config.userAuthenticationEnabled) {
					return true;
				}
				if (!this.isAuthenticated()) {
					return false;
				}
				if (this.currentCohortDefinition() && this.isNew()) {
					return this.canCreate();
				}

				return this.currentCohortDefinition();
			});

			this.canSave = ko.pureComputed(() => {
				return (
					(this.canEdit() && this.previewVersion()) ||
					(this.dirtyFlag().isDirty() &&
						!this.isRunning() &&
						this.canEdit() &&
						this.isNameCorrect())
				);
			});

			// TODO: Move this to conceptset-list?
			this.disableConceptSetExport = ko.pureComputed(() => {
				return (
					this.dirtyFlag().isDirty() ||
					(this.currentCohortDefinition() &&
						this.currentCohortDefinition().expression() &&
						this.currentCohortDefinition()
							.expression()
							.ConceptSets().length === 0)
				);
			});

			this.disableConceptSetExportMessage = ko.pureComputed(() => {
				if (
					this.currentCohortDefinition() &&
					this.currentCohortDefinition().expression().ConceptSets()
						.length === 0
				) {
					return ko.i18n(
						'cohortDefinitions.noConceptSets',
						'No concept sets to export'
					);
				}
				if (this.dirtyFlag().isDirty()) {
					return ko.i18n(
						'cohortDefinitions.saveDefinitionBefore',
						'You must save the definition before you can export'
					);
				}
			});

			this.criticalCount = ko.observable(0);
			this.isDiagnosticsRunning = ko.observable(false);

			this.warningParams = ko.observable({
				current: sharedState.CohortDefinition.current,
				warningsTotal: ko.observable(0),
				warningCount: ko.observable(0),
				infoCount: ko.observable(0),
				criticalCount: this.criticalCount,
				changeFlag: ko.pureComputed(() => this.dirtyFlag().isChanged()),
				isDiagnosticsRunning: this.isDiagnosticsRunning,
				onDiagnoseCallback: this.diagnose.bind(this),
				onFixCallback: this.fixConceptSet,
			});

			this.pollForInfoPeriodically = () => {
				this.pollTimeoutId = PollService.add({
					callback: () => this.pollForInfo(),
					interval: config.pollInterval,
				});
			};

			this.pollForInfo = () => {
				const { PENDING, RUNNING } = globalConstants.generationStatuses;
				if (
					this.currentCohortDefinition() &&
					!this.isNew() &&
					this.cohortDefinitionSourceInfo().some((i) =>
						[PENDING, RUNNING].includes(i.status())
					)
				) {
					var id = this.currentCohortDefinition().id();
					cohortDefinitionService.getInfo(id).then((infoList) => {
						infoList.forEach((info) => {
							// obtain source reference
							var source =
								this.cohortDefinitionSourceInfo().filter(
									(cdsi) => {
										var sourceId = sharedState
											.sources()
											.find(
												(source) =>
													source.sourceKey ==
													cdsi.sourceKey
											).sourceId;
										return sourceId === info.id.sourceId;
									}
								)[0];

							if (source) {
								if (source.status() !== info.status) {
									JobPollService.isJobListMutated(true);
								}
								source.status(info.status);
								source.isValid(info.isValid);
								source.startTime(
									momentApi.formatDateTime(
										new Date(info.startTime)
									)
								);
								source.createdBy(
									info.createdBy ? info.createdBy.login : null
								);

								if (
									info.status != 'COMPLETE' &&
									info.status != 'FAILED'
								) {
									// hasPending = true;
									source.executionDuration('...');
									source.personCount('...');
									source.recordCount('...');
								} else {
									var commaFormatted = d3.format(',');
									source.executionDuration(
										momentApi.formatDuration(
											info.executionDuration
										)
									);
									source.personCount(
										commaFormatted(info.personCount)
									);
									source.recordCount(
										commaFormatted(info.recordCount)
									);
									source.failMessage(info.failMessage);
								}
							}
						});
					});
				}
			};

			this.isRunning = ko.pureComputed(() => {
				return (
					this.cohortDefinitionSourceInfo().filter((info) => {
						return !(
							info.status() == 'COMPLETE' ||
							info.status() == 'n/a'
						);
					}).length > 0
				);
			});

			this.cohortDefinitionLink = ko.pureComputed(() => {
				if (this.currentCohortDefinition()) {
					return commonUtils.normalizeUrl(
						this.config.api.url,
						'cohortdefinition',
						this.currentCohortDefinition().id()
					);
				}
			});
			this.cohortLinkModalOpened = ko.observable(false);

			PermissionService.decorateComponent(this, {
				entityTypeGetter: () => entityType.COHORT_DEFINITION,
				entityIdGetter: () => this.currentCohortDefinition().id(),
				createdByUsernameGetter: () =>
					this.currentCohortDefinition() &&
					this.currentCohortDefinition().createdBy() &&
					this.currentCohortDefinition().createdBy().login,
			});

			TagsService.decorateComponent(this, {
				assetTypeGetter: () => TagsService.ASSET_TYPE.COHORT_DEFINITION,
				assetGetter: () => this.currentCohortDefinition(),
				addTagToAsset: (tag) => {
					const isDirty = this.dirtyFlag().isDirty();
					this.currentCohortDefinition().tags.push(tag);
					if (!isDirty) {
						this.dirtyFlag().reset();
						this.warningParams.valueHasMutated();
					}
				},
				removeTagFromAsset: (tag) => {
					const tags = this.currentCohortDefinition().tags,
						isDirty = this.dirtyFlag().isDirty();
					tags(
						tags().filter(
							(t) =>
								t.id !== tag.id &&
								tag.groups.filter((tg) => tg.id === t.id)
									.length === 0
						)
					);
					if (!isDirty) {
						this.dirtyFlag().reset();
						this.warningParams.valueHasMutated();
					}
				},
			});

			this.versionsParams = ko.observable({
				versionPreviewUrl: (versionNumber) =>
					`/cohortdefinition/${this.currentCohortDefinition().id()}/version/${versionNumber}`,
				currentVersion: () => {
					return {
						...this.currentCohortDefinition(),
						createdBy: this.currentCohortDefinition().createdBy(),
						createdDate:
							this.currentCohortDefinition().createdDate(),
						modifiedBy: this.currentCohortDefinition().modifiedBy(),
						modifiedDate:
							this.currentCohortDefinition().modifiedDate(),
					};
				},
				previewVersion: () => this.previewVersion(),
				getList: () =>
					cohortDefinitionService.getVersions(
						this.currentCohortDefinition().id()
					),
				updateVersion: (version) =>
					cohortDefinitionService.updateVersion(version),
				copyVersion: async (version) => {
					this.isCopying(true);
					try {
						const result =
							await cohortDefinitionService.copyVersion(
								this.currentCohortDefinition().id(),
								version.version
							);
						this.previewVersion(null);
						commonUtils.routeTo(constants.paths.details(result.id));
					} finally {
						this.isCopying(false);
					}
				},
				isAssetDirty: () => this.dirtyFlag().isDirty(),
				canAddComments: () => this.canEdit(),
			});

			this.pollForInfoPeriodically();
		}

		// METHODS
		delete() {
			if (
				!confirm(
					ko.i18n(
						'cohortDefinitions.cohortDefinitionManager.confirms.delete',
						'Delete cohort definition? Warning: deletion can not be undone!'
					)()
				)
			)
				return;

			this.isDeleting(true);

			// reset view after save
			cohortDefinitionService
				.deleteCohortDefinition(this.currentCohortDefinition().id())
				.then(
					(result) => {
						this.currentCohortDefinition(null);
						PollService.stop(this.pollTimeoutId);
						this.dirtyFlag().reset();
						this.close();
					},
					(error) => {
						console.log('Error: ' + error);
						if (error.status == 409) {
							alert(
								ko.i18n(
									'cohortDefinitions.cohortDefinitionManager.confirms.deleteConflict',
									'Cohort definition cannot be deleted because it is referenced in some analysis.'
								)()
							);
							this.isDeleting(false);
						} else {
							authApi.handleAccessDenied(error);
						}
					}
				);
		}

		async save() {
			if (
				this.previewVersion() &&
				!confirm(
					ko.i18n(
						'common.savePreviewWarning',
						'Save as current version?'
					)()
				)
			) {
				return;
			}

			this.isSaving(true);

			const cohortDefinitionName = this.currentCohortDefinition().name();
			this.currentCohortDefinition().name(cohortDefinitionName.trim());

			// Next check to see that a cohort definition with this name does not already exist
			// in the database. Also pass the id so we can make sure that the
			// current Cohort Definition is excluded in this check.

			try {
				const results = await cohortDefinitionService.exists(
					this.currentCohortDefinition().name(),
					this.currentCohortDefinition().id()
				);
				if (results > 0) {
					alert(
						ko.i18n(
							'cohortDefinitions.cohortDefinitionManager.confirms.saveAlert',
							'A cohort definition with this name already exists. Please choose a different name.'
						)()
					);
				} else {
					this.conceptSetStore.clear();

					// If we are saving a new cohort definition (id === 0) then clear
					// the id field before saving
					if (this.currentCohortDefinition().id() === '0') {
						this.currentCohortDefinition().id(undefined);
					}
					var definition = ko.toJS(this.currentCohortDefinition());
					// reset view after save
					const savedDefinition =
						await cohortDefinitionService.saveCohortDefinition(
							definition
						);
					definition = new CohortDefinition(savedDefinition);
					const redirectWhenComplete =
						definition.id() != this.currentCohortDefinition().id();
					this.currentCohortDefinition(definition);
					this.previewVersion(null);
					this.versionsParams.valueHasMutated();
					if (redirectWhenComplete) {
						commonUtils.routeTo(
							constants.paths.details(definition.id())
						);
					}
				}
				sharedState.CohortDefinition.lastUpdatedId(
					this.currentCohortDefinition().id()
				);
			} catch (e) {
				console.error(e);
				alert(
					ko.i18n(
						'cohortDefinitions.cohortDefinitionManager.confirms.saveError',
						'An error occurred while attempting to save a cohort definition.'
					)()
				);
			} finally {
				this.isSaving(false);
			}
		}

		close() {
			if (
				this.dirtyFlag().isDirty() &&
				!confirm(
					ko.i18n(
						'cohortDefinitions.cohortDefinitionManager.confirms.close',
						'Your cohort changes are not saved. Would you like to continue?'
					)()
				)
			) {
				return;
			} else {
				this.conceptSetStore.clear();
				this.currentCohortDefinition(null);
				this.previewVersion(null);
				this.dirtyFlag().reset();
				commonUtils.routeTo('/cohortdefinitions');
			}
		}

		async copy() {
			this.isCopying(true);
			// reset view after save
			try {
				const result =
					await cohortDefinitionService.copyCohortDefinition(
						this.currentCohortDefinition().id()
					);
				commonUtils.routeTo(constants.paths.details(result.id));
			} finally {
				this.isCopying(false);
			}
		}

		getSourceId(sourceKey) {
			return sharedState
				.sources()
				.find((source) => source.sourceKey === sourceKey).sourceId;
		}

		hasCDM(source) {
			return source.daimons.some((daimon) => daimon.daimonType === 'CDM');
		}

		hasResults(source) {
			return source.daimons.some(
				(daimon) => daimon.daimonType === 'Results'
			);
		}

		removeConceptSet(id) {
			this.currentCohortDefinition()
				.expression()
				.ConceptSets.remove(function (item) {
					return item.id === id;
				});
		}

		removeInclusionRule(name) {
			this.currentCohortDefinition()
				.expression()
				.InclusionRules.remove((item) => item.name() === name);
		}

		fixConceptSet(warning) {
			if (
				warning.type === 'ConceptSetWarning' &&
				warning.conceptSetId >= 0
			) {
				this.removeConceptSet(warning.conceptSetId);
			} else if (
				warning.type === 'IncompleteRuleWarning' &&
				warning.ruleName
			) {
				this.removeInclusionRule(warning.ruleName);
			}
		}

		diagnose() {
			if (this.currentCohortDefinition()) {
				return cohortDefinitionService.runDiagnostics(
					this.currentCohortDefinition()
				);
			}
		}

		onRouterParamsChanged(params) {
			let {
				cohortDefinitionId,
				mode = 'definition',
				sourceKey,
				sampleId,
				version,
			} = params;
			// cohortDefinitionId can be undefined in case of following links from notifications
			// when another tab of the same cohort definition is selected
			if (!cohortDefinitionId && this.currentCohortDefinition()) {
				cohortDefinitionId = this.currentCohortDefinition().id();
			}
			this.tabMode(mode);

			this.selectedSourceKey(sourceKey);
			this.selectedSampleId(sampleId);

			if (version === 'current' || !this.currentCohortDefinition() || !this.currentCohortDefinitionInfo()) {
				this.previewVersion(null);
				this.prepareCohortDefinition(cohortDefinitionId);
			} else if (version) {
				this.prepareCohortDefinition(cohortDefinitionId, version);
			}
		}

		backToCurrentVersion() {
			if (
				this.dirtyFlag().isDirty() &&
				!confirm(
					ko.i18n(
						'common.unsavedWarning',
						'Unsaved changes will be lost. Proceed?'
					)()
				)
			) {
				return;
			}
			commonUtils.routeTo(
				`/cohortdefinition/${this.currentCohortDefinition().id()}/version/current`
			);
		}

		getSourceInfo(source) {
			const info = this.currentCohortDefinitionInfo();
			return info.find((info) => info.id.sourceId === source.sourceId);
		}

		getCDSI(source, sourceInfo) {
			const cdsi = {};
			cdsi.name = source.sourceName;
			cdsi.sourceKey = source.sourceKey;
			cdsi.sourceId = source.sourceId;
			if (sourceInfo != null) {
				cdsi.isValid = ko.observable(sourceInfo.isValid);
				cdsi.isCanceled = ko.observable(sourceInfo.isCanceled);
				cdsi.status = ko.observable(sourceInfo.status);
				const date = new Date(sourceInfo.startTime);
				cdsi.startTime = ko.observable(momentApi.formatDateTime(date));
				cdsi.executionDuration = ko.observable(
					momentApi.formatDuration(sourceInfo.executionDuration)
				);
				const commaFormatted = d3.format(',');
				if (sourceInfo.personCount == null) {
					cdsi.personCount = ko.observable('...');
				} else {
					cdsi.personCount = ko.observable(
						commaFormatted(sourceInfo.personCount)
					);
				}
				if (sourceInfo.recordCount) {
					cdsi.recordCount = ko.observable(
						commaFormatted(sourceInfo.recordCount)
					);
				} else {
					cdsi.recordCount = ko.observable('...');
				}
				cdsi.failMessage = ko.observable(sourceInfo.failMessage);
				cdsi.createdBy = ko.observable(sourceInfo.createdBy);
			} else {
				cdsi.isValid = ko.observable(false);
				cdsi.isCanceled = ko.observable(false);
				cdsi.status = ko.observable('n/a');
				cdsi.startTime = ko.observable('n/a');
				cdsi.executionDuration = ko.observable('n/a');
				cdsi.personCount = ko.observable('n/a');
				cdsi.recordCount = ko.observable('n/a');
				cdsi.failMessage = ko.observable(null);
				cdsi.createdBy = ko.observable(null);
			}
			return cdsi;
		}

		async loadRequiredData() {
			if (this.currentCohortDefinition()) {
				try {
					// now that we have required information lets compile them into data objects for our view
					const cdmSources = sharedState
						.sources()
						.filter(commonUtils.hasCDM);
					const results = cdmSources.map((source) => {
						const sourceInfo = this.getSourceInfo(source);
						return this.getCDSI(source, sourceInfo);
					});
					this.cohortDefinitionSourceInfo(results);
				} catch (er) {
					console.error(er);
				}
			}
		}

		async prepareCohortDefinition(cohortDefinitionId, versionNumber) {
			this.isLoading(true);
			if (parseInt(cohortDefinitionId) === 0) {
				this.setNewCohortDefinition();
			} else if (versionNumber) {
				const cohortDefinition =
					await cohortDefinitionService.getVersion(
						cohortDefinitionId,
						versionNumber
					);
				this.currentCohortDefinition(
					new CohortDefinition(cohortDefinition)
				);
				this.previewVersion(cohortDefinition.versionDef);
				const generationInfo = await cohortDefinitionService.getInfo(
					cohortDefinitionId
				);
				this.currentCohortDefinitionInfo(generationInfo);
			} else {
				await this.loadExistingCohortDefinition(cohortDefinitionId);
			}
			await this.loadRequiredData();

			this.isLoading(false);
		}

		setNewCohortDefinition() {
			this.currentCohortDefinition(
				new CohortDefinition({ id: 0, name: 'New Cohort Definition' })
			);
			this.currentCohortDefinitionInfo([]);
		}

		async loadExistingCohortDefinition(id) {
			try {
				const cohortDefinition =
					await cohortDefinitionService.getCohortDefinition(id);
				const generationInfo = await cohortDefinitionService.getInfo(
					id
				);
				this.currentCohortDefinition(
					new CohortDefinition(cohortDefinition)
				);
				this.currentCohortDefinitionInfo(generationInfo);
			} catch (err) {
				console.error(err);
			}
		}

		loadConceptSet(conceptSetId) {
			this.conceptSetStore.current(
				this.conceptSets()().find((item) => item.id == conceptSetId)
			);
			this.conceptSetStore.isEditable(this.canEdit());
			commonUtils.routeTo(
				`/cohortdefinition/${this.currentCohortDefinition().id()}/conceptsets/`
			);
		}

		exportConceptSetsCSV() {
			return FileService.loadZip(
				`${
					config.api.url
				}cohortdefinition/${this.currentCohortDefinition().id()}/export/conceptset`,
				`cohortdefinition-conceptsets-${this.currentCohortDefinition().id()}.zip`
			);
		}

		selectTab(key) {
			this.tabMode(key);
			if (key === 'samples') {
				return commonUtils.routeTo(
					'/cohortdefinition/' +
						this.currentCohortDefinition().id() +
						'/' +
						key + 
						'/' +
						this.cdmSources()[0].sourceKey
				);
			}
			return commonUtils.routeTo(
				'/cohortdefinition/' +
					this.currentCohortDefinition().id() +
					'/' +
					key
			);
		}

		dispose() {
			PollService.stop(this.pollTimeoutId);
			super.dispose();
		}

		getAuthorship() {
			const cohortDef = this.currentCohortDefinition();

			let createdText, createdBy, createdDate, modifiedBy, modifiedDate;

			if (this.previewVersion()) {
				createdText = ko.i18n(
					'components.authorship.versionCreated',
					'version created'
				);
				createdBy = this.previewVersion().createdBy
					? this.previewVersion().createdBy.name
					: ko.i18n('common.anonymous', 'anonymous');
				createdDate = commonUtils.formatDateForAuthorship(
					this.previewVersion().createdDate
				);
				modifiedBy = null;
				modifiedDate = null;
			} else {
				createdText = ko.i18n(
					'components.authorship.created',
					'created'
				);
				createdBy = cohortDef.createdBy()
					? cohortDef.createdBy().name
					: ko.i18n('common.anonymous', 'anonymous');
				createdDate = commonUtils.formatDateForAuthorship(
					cohortDef.createdDate
				);
				modifiedBy = cohortDef.modifiedBy()
					? cohortDef.modifiedBy().name
					: ko.i18n('common.anonymous', 'anonymous');
				modifiedDate = commonUtils.formatDateForAuthorship(
					cohortDef.modifiedDate
				);
			}

			return {
				createdText: createdText,
				createdBy: createdBy,
				createdDate: createdDate,
				modifiedBy: modifiedBy,
				modifiedDate: modifiedDate,
			};
		}
	}

	return commonUtils.build(
		'cohort-definition-manager',
		CohortDefinitionManager,
		view
	);
});

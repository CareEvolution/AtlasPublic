define([
	'knockout',
	'text!./source-manager.html',
	'pages/Page',
	'utils/AutoBind',
	'utils/CommonUtils',
	'appConfig',
	'services/Vocabulary',
	'assets/ohdsi.util',
	'services/SourceAPI',
	'lodash',
	'services/AuthAPI',
	'atlas-state',
	'pages/configuration/const',
	'components/ac-access-denied',
	'less!./source-manager.less',
	'components/editable-heading',
], function (
	ko,
	view,
	Page,
	AutoBind,
	commonUtils,
	config,
	vocabularyProvider,
	ohdsiUtil,
	sourceApi,
	lodash,
	authApi,
	sharedState,
	constants
) {
	// Should we translate daimons?
	var defaultDaimons = {
		CDM: {
			tableQualifier: '',
			enabled: false,
			priority: 0,
			sourceDaimonId: null,
		},
		Vocabulary: {
			tableQualifier: '',
			enabled: false,
			priority: 0,
			sourceDaimonId: null,
		},
		Results: {
			tableQualifier: '',
			enabled: false,
			priority: 0,
			sourceDaimonId: null,
		},
		CEM: {
			tableQualifier: '',
			enabled: false,
			priority: 0,
			sourceDaimonId: null,
		},
		CEMResults: {
			tableQualifier: '',
			enabled: false,
			priority: 0,
			sourceDaimonId: null,
		},
		Temp: {
			tableQualifier: '',
			enabled: false,
			priority: 0,
			sourceDaimonId: null,
		},
	};

	function Source(data) {
		function mapDaimons(daimons) {
			daimons = daimons || [];
			var defaultKeys = Object.keys(defaultDaimons);
			var keys = daimons.map(function (daimon) {
				return daimon.daimonType;
			});
			var result = daimons.map(function (daimon) {
				return {
					...lodash.omit(daimon, ['tableQualifier']),
					tableQualifier: ko.observable(daimon.tableQualifier),
					enabled:
						daimon.daimonType !== 'Vocabulary'
							? ko.observable(true)
							: ko.observable(false),
				};
			});
			var diff = lodash.difference(defaultKeys, keys).map(function (key) {
				return {
					...defaultDaimons[key],
					tableQualifier: ko.observable(''),
					daimonType: key,
					enabled:
						key !== 'Vocabulary'
							? ko.observable(true)
							: ko.observable(false),
				};
			});
			return lodash.concat(result, diff);
		}

		data = data || {};

		this.sourceId = ko.observable(data.sourceId || 0);
		this.name = ko.observable(
			data.sourceName ||
				ko.unwrap(ko.i18n('configuration.newSource', 'New Source'))
		);
		this.key = ko.observable(data.sourceKey || null);
		this.dialect = ko.observable(data.sourceDialect || null);
		this.connectionString = ko.observable(data.connectionString || null);
		this.username = ko.observable(data.username || null);
		this.password = ko.observable(data.password || null);
		this.daimons = ko.observableArray(mapDaimons(data.daimons));
		this.keyfileName = ko.observable(data.keyfileName);
		this.krbAuthMethod = ko.observable(data.krbAuthMethod);
		this.krbAdminServer = ko.observable(data.krbAdminServer);
		this.canDelete = ko.observable(data.hasDeleteAccess);
		this.canEdit = ko.observable(data.hasWriteAccess || this.sourceId() === 0);

		return this;
	}

	class SourceManager extends AutoBind(Page) {
		constructor(params) {
			super(params);
			this.config = config;
			this.loading = ko.observable(false);
			this.dirtyFlag = sharedState.ConfigurationSource.dirtyFlag;
			this.selectedSource = sharedState.ConfigurationSource.current;
			this.selectedSourceId = sharedState.ConfigurationSource.selectedId;
			this.dialectOptions = ko.observableArray([]);
			this.isAuthenticated = authApi.isAuthenticated;
			this.appInitializationStatus = sharedState.appInitializationStatus;
			this.revealPassword = ko.observable(false);

			this.hasAccess = ko.pureComputed(() => {
				return !config.userAuthenticationEnabled || this.isAuthenticated();
			});

			this.canReadSource = ko.observable(true);

			this.isDeletePermitted = ko.pureComputed(() => {
				return (
					this.selectedSource() && this.selectedSource().canDelete()
				);
			});

			this.canEdit = ko.pureComputed(() => {
				return this.selectedSource() && this.selectedSource().canEdit();
			});

			this.isNew = ko.pureComputed(() => {
				return parseInt(this.selectedSourceId()) === 0;
			});

			this.canSave = ko.pureComputed(() => {
				return (
					this.selectedSource() &&
					this.selectedSource().name() &&
					this.selectedSource().key() &&
					this.selectedSource().connectionString() &&
					this.canEdit()
				);
			});

			this.canDelete = () => {
				return (
					this.selectedSource() &&
					!this.isNew() &&
					this.selectedSource().key() &&
					this.isDeletePermitted()
				);
			};

			this.canEditKey = ko.pureComputed(this.isNew);

			sourceApi.getSupportedDialects().then((dialects) => {
				const translatedOptions = dialects.map((d) => ({
					id: d.id,
					name: ko.i18n(d.i18nKey, d.label),
				}));
				this.dialectOptions(translatedOptions);
			});

			this.sourceCaption = ko.computed(() => {
				return this.selectedSource() == null ||
					this.selectedSource().key() == null
					? ko.i18n('const.newEntityNames.source', 'New Source')
					: ko.i18nformat(
							'configuration.viewEdit.source.title',
							'Source <%=name%>',
							{ name: this.selectedSource().name() }
					  );
			});

			this.isKrbAuth = ko.computed(() => {
				return this.impalaConnectionStringIncludes('AuthMech=1');
			});

			this.krbHostFQDN = ko.computed(() => {
				if (this.isImpalaDS() && this.isNonEmptyConnectionString()) {
					var str = this.selectedSource().connectionString();
					var strArray = str.match(/KrbHostFQDN=(.*?);/);
					if (strArray != null) {
						var matchedStr = strArray[0];
						return matchedStr.substring(
							matchedStr.search('=') + 1,
							matchedStr.length - 1
						);
					} else {
						return '';
					}
				}
				return '';
			});

			this.krbRealm = ko.computed(() => {
				if (this.isImpalaDS() && this.isNonEmptyConnectionString()) {
					var str = this.selectedSource().connectionString();
					var strArray = str.match(/KrbRealm=(.*?);/);
					if (strArray != null) {
						var matchedStr = strArray[0];
						return matchedStr.substring(
							matchedStr.search('=') + 1,
							matchedStr.length - 1
						);
					} else {
						return '';
					}
				}
				return '';
			});

			this.fieldsVisibility = {
				username: ko.computed(
					() => !this.supportsKeyfileAuth() || this.isKrbAuth()
				),
				password: ko.computed(() => !this.supportsKeyfileAuth()),
				krbAuthSettings: ko.computed(() => this.isKrbAuth()),
				krbFileInput: ko.computed(() => {
					return (
						this.isKrbAuth() &&
						(typeof this.selectedSource().keyfileName() !==
							'string' ||
							this.selectedSource().keyfileName().length === 0)
					);
				}),
				showKrbKeyfile: ko.computed(
					() =>
						this.isImpalaDS() &&
						this.selectedSource().krbAuthMethod() === 'keytab'
				),
				bigQueryAuthSettings: ko.computed(() => this.isBigQueryDS()),
				bqFileInput: ko.computed(() => {
					return (
						this.isBigQueryDS() &&
						(typeof this.selectedSource().keyfileName() !==
							'string' ||
							this.selectedSource().keyfileName().length === 0)
					);
				}),

				// warnings
				hostWarning: ko.computed(() => {
					var showWarning =
						this.isKrbAuth() && this.krbHostFQDN() === '';
					if (showWarning) {
						this.dirtyFlag().reset();
					}
					return showWarning;
				}),
				realmWarning: ko.computed(() => {
					var showWarning =
						this.isKrbAuth() && this.krbRealm() === '';
					if (showWarning) {
						this.dirtyFlag().reset();
					}
					return showWarning;
				}),
				userWarning: ko.computed(() => {
					var showWarning =
						this.selectedSource() != null &&
						this.selectedSource().username() === '';
					if (showWarning) {
						this.dirtyFlag().reset();
					}
					return showWarning;
				}),
			};
		}

		newSource() {
			this.selectedSource(new Source());
			this.dirtyFlag(new ohdsiUtil.dirtyFlag(this.selectedSource()));
		}

		supportsKeyfileAuth() {
			return this.isImpalaDS() || this.isBigQueryDS();
		}

		isImpalaDS() {
			return (
				this.selectedSource() &&
				this.selectedSource().dialect() === 'impala'
			);
		}

		isBigQueryDS() {
			return (
				this.selectedSource() &&
				this.selectedSource().dialect() === 'bigquery'
			);
		}

		isNonEmptyConnectionString() {
			return (
				this.selectedSource() != null &&
				typeof this.selectedSource().connectionString() === 'string' &&
				this.selectedSource().connectionString().length > 0
			);
		}

		impalaConnectionStringIncludes(substr) {
			return (
				this.isImpalaDS() &&
				this.isNonEmptyConnectionString() &&
				this.selectedSource().connectionString().includes(substr)
			);
		}

		removeKeyfile() {
			$('#keyfile').val(''); // TODO: create "ref" directive
			this.keyfile = null;
			this.selectedSource().keyfileName(null);
		}

		uploadFile(file) {
			this.keyfile = file;
			this.selectedSource().keyfileName(file.name);
		}

		async save() {
			const source = {
				name: this.selectedSource().name() || null,
				key: this.selectedSource().key() || null,
				dialect: this.selectedSource().dialect() || null,
				connectionString:
					this.selectedSource().connectionString() || null,
				krbAuthMethod:
					this.selectedSource().krbAuthMethod() || 'password',
				krbAdminServer: this.selectedSource().krbAdminServer() || null,
				username: this.selectedSource().username() || null,
				password: this.selectedSource().password() || null,
				daimons: ko
					.toJS(this.selectedSource().daimons())
					.filter(function (d) {
						return d.tableQualifier && d.tableQualifier.trim().length > 0;
					})
					.map(function (d) {
						return lodash.omit(d, ['enabled']);
					}),
				keyfileName: this.selectedSource().keyfileName(),
				keyfile: this.keyfile,
			};
			try {
				this.loading(true);
				const sourceId = this.isNew() ? null : this.selectedSourceId();
				await sourceApi.saveSource(sourceId, source);
				const appStatus = await sourceApi.initSourcesConfig();
				this.appInitializationStatus(appStatus);
				await vocabularyProvider.getDomains();
				this.goToConfigure();
			} catch ({ data = {} }) {
				this.loading(false);
				const {
					payload: {
						message = ko.unwrap(
							ko.i18n(
								'configuration.viewEdit.source.alerts.save.errorMessage',
								'Please contact your administrator to resolve this issue.'
							)
						),
					} = {},
				} = data;
				alert(
					ko.unwrap(
						ko.i18nformat(
							'configuration.viewEdit.source.alerts.save.error',
							'The Source was not saved. <%= message %>',
							{ message: message }
						)
					)
				);
			}
		}

		close() {
			if (
				this.dirtyFlag().isDirty() &&
				!confirm(
					ko.unwrap(
						ko.i18n(
							'configuration.viewEdit.source.confirms.close',
							'Source changes are not saved. Would you like to continue?'
						)
					)
				)
			) {
				return;
			}
			this.goToConfigure();
		}

		hasPriorityDaimons() {
			// This source has priority daimons if at least one of its prioritizable daimons has a higher
			// priority than all other sources' prioritizable daimons of the same type

			const otherSources = sharedState
				.sources()
				.filter((s) => s.sourceId !== this.selectedSource().sourceId);
			const otherPrioritizableDaimons = lodash.flatten(
				otherSources.map((s) =>
					s.daimons.filter(
						(d) =>
							constants.prioritizableDaimonTypes.includes(
								d.daimonType
							) && d.sourceDaimonId
					)
				)
			);
			const currentPrioritizableDaimons = this.selectedSource()
				.daimons()
				.filter(
					(d) =>
						constants.prioritizableDaimonTypes.includes(
							d.daimonType
						) && d.sourceDaimonId
				);

			return currentPrioritizableDaimons.some((currentDaimon) => {
				return otherPrioritizableDaimons
					.filter(
						(otherDaimon) =>
							otherDaimon.daimonType === currentDaimon.daimonType
					)
					.every(
						(otherDaimon) =>
							currentDaimon.priority > otherDaimon.priority
					);
			});
		}

		async delete() {
			if (this.hasPriorityDaimons()) {
				alert(
					ko.unwrap(
						ko.i18n(
							'configuration.viewEdit.source.alerts.delete.hasSelectedPriotirizableDaimons',
							'Some daimons of this source were given highest priority and are in use by application. Select new top-priority diamons to delete the source.'
						)
					)
				);
				return;
			}

			if (
				!confirm(
					ko.unwrap(
						ko.i18n(
							'configuration.viewEdit.source.confirms.delete',
							'Delete source? Warning: deletion can not be undone!'
						)
					)
				)
			) {
				return;
			}
			try {
				this.loading(true);
				await sourceApi.deleteSource(this.selectedSourceId());
				const appStatus = await sourceApi.initSourcesConfig();
				this.appInitializationStatus(appStatus);
				this.goToConfigure();
			} catch (err) {
				console.error(err);
			} finally {
				this.loading(false);
			}
		}

		goToConfigure() {
			this.selectedSource(null);
			this.selectedSourceId(null);
			this.dirtyFlag().reset();
			commonUtils.routeTo('/configure');
		}

		onRouterParamsChanged(params = {}) {
			const selectedSourceId = parseInt(this.selectedSourceId());
			if (this.isNew() && !this.dirtyFlag().isDirty()) {
				this.newSource();
			} else if (
				selectedSourceId > 0 &&
				selectedSourceId !==
					(this.selectedSource() && this.selectedSource().sourceId())
			) {
				this.onSourceSelected();
			}
		}

		async onSourceSelected() {
			if (this.hasAccess()) {
				try {
					this.loading(true);
					const source = await sourceApi.getSource(
						this.selectedSourceId()
					);
					this.selectedSource(new Source(source));
					this.dirtyFlag(
						new ohdsiUtil.dirtyFlag(this.selectedSource())
					);
				} catch (e) {
					console.error(e);
					alert(
						ko.unwrap(
							ko.i18nformat(
								'configuration.viewEdit.source.alerts.sourceSelectedError',
								'An error occurred while attempting to get a source with id=<%=selectedSourceId%>.',
								{ selectedSourceId: this.selectedSourceId() }
							)
						)
					);
					commonUtils.routeTo('/configure');
				} finally {
					this.loading(false);
				}
			}
		}

		daimonTableQualifierUpdated(daimon) {
			if (daimon.daimonType === 'CDM') {
				const vocabularyDaimon = this.selectedSource().daimons().find(
					(d) => d.daimonType === 'Vocabulary'
				);
				if (vocabularyDaimon) {
					vocabularyDaimon.tableQualifier(daimon.tableQualifier());
				}
			}
		}

		toggleRevealPassword() {
			this.revealPassword(!this.revealPassword());
		}
	}

	return commonUtils.build('source-manager', SourceManager, view);
});

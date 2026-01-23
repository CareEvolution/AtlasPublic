define(function (require, exports) {
	var $ = require('jquery');
	var config = require('appConfig');
	var ko = require('knockout');
	const httpService = require('services/http');

	const AUTH_PROVIDERS = {
		IAP: 'AtlasGoogleSecurity',
	};

	const AUTH_CLIENTS = {
		SAML: 'AUTH_CLIENT_SAML',
	};

	const signInOpened = ko.observable(false);

	var getServiceUrl = function () {
		return config.webAPIRoot;
	};

	var token = ko.observable(null);
	const tokenExpired = ko.observable(false);

	var authClient = ko.computed({
		owner: ko.observable(localStorage.getItem('auth-client')),
		read: function () {
			return this();
		},
		write: function (newValue) {
			localStorage.setItem('auth-client', newValue);
			this(newValue);
		},
	});

	var reloginRequired = ko.observable(false);
	var subject = ko.observable();
	var fullName = ko.observable();
	const authProvider = ko.observable();

	authProvider.subscribe((provider) => {
		if (provider === AUTH_PROVIDERS.IAP) {
			const id = 'google-iap-refresher';
			const iframe = `<iframe id="${id}" src="/_gcp_iap/session_refresher" style="position: absolute; width:0;height:0;border:0; border:none;"></iframe>`;
			$('#' + id).remove();
			$('body').append(iframe);
		}
	});

	const loadUserInfo = function () {
		return httpService.doGet(getServiceUrl() + 'user/me').then(({ data, headers }) => {
				subject(data.login);
				authProvider(headers.get('x-auth-provider'));
				fullName(data.name ? data.name : data.login);
			},
			(error) => {
				if (error.status === 401 || error.status === 403) {
					console.log('User is not authenticated or authorized');
					subject(null);
					if (config.enableSkipLogin) {
						signInOpened(true);
					}
				} else {
					throw new Error('Cannot retrieve user info');
				}
			})
	};

	var isAuthenticated = ko.computed(() => {
		if (!config.userAuthenticationEnabled) {
			return true;
		}

		return !!subject();
	});

	var handleAccessDenied = function (xhr) {
		switch (xhr.status) {
			case 401:
				resetAuthParams();
				break;
			case 403:
				break;
		}
	};

	var isPermitted = function (permission) {
		if (!config.userAuthenticationEnabled) {
			return true;
		}
		return false;
	};

	var isPermittedReadIRs = function () {
		return isPermitted('ir:get');
	};

	var isPermittedEditIR = function (id) {
		return isPermitted('ir:' + id + ':put');
	};

	var isPermittedCreateIR = function () {
		return isPermitted('ir:post');
	};

	var isPermittedDeleteIR = function (id) {
		return isPermitted('ir:' + id + ':delete');
	};

	var isPermittedCopyIR = function (id) {
		return isPermitted('ir:' + id + ':copy:get');
	};

	var isPermittedReadEstimations = function () {
		return isPermitted('comparativecohortanalysis:get');
	};

	var isPermittedReadEstimation = function (id) {
		return isPermitted('comparativecohortanalysis:' + id + ':get');
	};

	var isPermittedCreateEstimation = function () {
		return isPermitted('comparativecohortanalysis:post');
	};

	const isPermittedDeleteEstimation = function (id) {
		return isPermitted(`comparativecohortanalysis:${id}:delete`);
	};

	var isPermittedReadPlps = function () {
		return isPermitted('plp:get');
	};

	var isPermittedCreatePlp = function () {
		return isPermitted('plp:post');
	};

	var isPermittedReadPlp = function (id) {
		return isPermitted('plp:' + id + ':get');
	};

	var isPermittedDeletePlp = function (id) {
		return isPermitted('plp:' + id + ':delete');
	};

	var isPermittedCopyPlp = function (id) {
		return isPermitted(`plp:${id}:copy:get`);
	};

	const isPermittedImportUsers = function () {
		return (
			isPermitted('user:import:post') && isPermitted('user:import:*:post')
		);
	};

	const isPermittedRunAs = () => isPermitted('user:runas:post');

	const setAuthParams = (newToken) => {
		!!newToken && token(newToken);
	};

	var resetAuthParams = function () {
		token(null);
		subject(null);
	};

	const runAs = function (login, success, error) {
		return httpService.doPost(
			config.webAPIRoot + 'user/runas',
			{ login }
		).then(({data}) => success(data)).catch(error);
	};

	const executeWithRefresh = async function (httpPromise) {
		const result = await httpPromise;
		return result;
	};

	const canCreateSource = function() {
		return httpService.doGet(config.webAPIRoot + 'capabilities/createSource')
			.then(res => res.data);
	}

	const canCreateTag = function() {
		return httpService.doGet(config.webAPIRoot + 'capabilities/createTag')
			.then(res => res.data);
	}

	const canCreateCohortDefinition = function() {
		return httpService.doGet(config.webAPIRoot + 'capabilities/createCohortDefinition')
			.then(res => res.data);
	}

	const canClearAllSourcesCache = function() {
		return httpService.doGet(config.webAPIRoot + 'capabilities/clearAllSourcesCache')
			.then(res => res.data);
	}

	const canSearchVocabulary = function() {
		return httpService.doGet(config.webAPIRoot + 'capabilities/searchVocabulary')
			.then(res => res.data);
	}

	const canReadPerson = function() {
		return httpService.doGet(config.webAPIRoot + 'capabilities/readPerson')
			.then(res => res.data);
	}

	const canReadPersonDates = function() {
		return httpService.doGet(config.webAPIRoot + 'capabilities/readPersonDates')
			.then(res => res.data);
	}

	const canReadNotifications = function() {
		return httpService.doGet(config.webAPIRoot + 'capabilities/readNotifications')
			.then(res => res.data);
	}

	const canUpdateNotifications = function() {
		return httpService.doGet(config.webAPIRoot + 'capabilities/updateNotifications')
			.then(res => res.data);
	}

	const canReadRoles = function() {
		return httpService.doGet(config.webAPIRoot + 'capabilities/readRoles')
			.then(res => res.data);
	}

	const canCreateRole = function() {
		return httpService.doGet(config.webAPIRoot + 'capabilities/createRole')
			.then(res => res.data);
	}

	const canUpdateRole = function() {
		return httpService.doGet(config.webAPIRoot + 'capabilities/updateRole')
			.then(res => res.data);
	}

	const canDeleteRole = function() {
		return httpService.doGet(config.webAPIRoot + 'capabilities/deleteRole')
			.then(res => res.data);
	}

	const canCreateConceptSet = function() {
		return httpService.doGet(config.webAPIRoot + 'capabilities/createConceptSet')
			.then(res => res.data);
	}

	const canCreateCohortCharacterization = function() {
		return httpService.doGet(config.webAPIRoot + 'capabilities/createCohortCharacterization')
			.then(res => res.data);
	}

	const canCreateFeatureAnalysis = function() {
		return httpService.doGet(config.webAPIRoot + 'capabilities/createFeatureAnalysis')
			.then(res => res.data);
	}

	var api = {
		AUTH_PROVIDERS: AUTH_PROVIDERS,
		AUTH_CLIENTS: AUTH_CLIENTS,

		token: token,
		authClient: authClient,
		reloginRequired: reloginRequired,
		subject: subject,
		fullName,
		tokenExpired: tokenExpired,
		authProvider: authProvider,
		setAuthParams: setAuthParams,
		resetAuthParams: resetAuthParams,
		handleAccessDenied: handleAccessDenied,

		isAuthenticated: isAuthenticated,
		signInOpened: signInOpened,

		//////// Leaving these as is even though they will now always return false
		isPermitted: isPermitted,

		isPermittedReadIRs: isPermittedReadIRs,
		isPermittedCreateIR: isPermittedCreateIR,
		isPermittedEditIR: isPermittedEditIR,
		isPermittedDeleteIR: isPermittedDeleteIR,
		isPermittedCopyIR,

		isPermittedReadEstimations: isPermittedReadEstimations,
		isPermittedReadEstimation: isPermittedReadEstimation,
		isPermittedCreateEstimation: isPermittedCreateEstimation,
		isPermittedDeleteEstimation,

		isPermittedReadPlps: isPermittedReadPlps,
		isPermittedReadPlp: isPermittedReadPlp,
		isPermittedCreatePlp: isPermittedCreatePlp,
		isPermittedDeletePlp: isPermittedDeletePlp,
		isPermittedCopyPlp: isPermittedCopyPlp,

		isPermittedImportUsers,
		isPermittedRunAs,
		/////////////////////////

		loadUserInfo,
		runAs,
		executeWithRefresh,

		// These are the new permission checks
		canCreateSource,
		canCreateTag,
		canCreateCohortDefinition,
		canClearAllSourcesCache,
		canSearchVocabulary,
		canReadPerson,
		canReadPersonDates,
		canReadNotifications,
		canUpdateNotifications,
		canReadRoles,
		canCreateRole,
		canUpdateRole,
		canDeleteRole,
		canCreateConceptSet,
		canCreateCohortCharacterization,
		canCreateFeatureAnalysis,
	};

	return api;
});

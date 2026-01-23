define([
	'require',
	'jquery',
	'knockout',
	'text!./welcome.html',
	'appConfig',
	'services/AuthAPI',
	'utils/BemHelper',
	'atlas-state',
	'services/MomentAPI',
	'less!welcome.less',
], function (require, $, ko, view, appConfig, authApi, BemHelper, sharedState, momentApi) {

	const httpService = require('services/http');

	const componentName = 'welcome';

	function welcome(params) {
		const self = this;
		const bemHelper = new BemHelper(componentName);
		this.classes = bemHelper.run.bind(bemHelper);
		self.token = authApi.token;
		self.reloginRequired = authApi.reloginRequired;
		self.loadUserInfo = authApi.loadUserInfo;
		self.setAuthParams = authApi.setAuthParams;
		self.resetAuthParams = authApi.resetAuthParams;
		self.serviceUrl = appConfig.webAPIRoot;
		self.errorMsg = ko.observable();
		self.isInProgress = ko.observable(false);
		self.login = authApi.subject;
		self.isDbLoginAtt = ko.observable(false);
		self.authUrl = ko.observable();
		self.isBadCredentials = ko.observable(false);
		self.tokenExpired = authApi.tokenExpired;
		self.isLoggedIn = authApi.isAuthenticated;
		self.isPermittedRunAs = ko.computed(
			() => self.isLoggedIn() && authApi.isPermittedRunAs()
		);
		self.runAsLogin = ko.observable();
		self.isGoogleIapAuth = ko.computed(
			() => authApi.authProvider() === authApi.AUTH_PROVIDERS.IAP
		);
		self.status = ko.computed(function () {
			if (self.isInProgress())
				return ko.i18n('components.welcome.wait', 'Please wait...')();
			if (self.errorMsg()) return self.errorMsg();
			if (self.isLoggedIn()) {
				return ko.i18nformat(
						'components.welcome.loggedIn',
						"Logged in as '<%=login%>'",
						{ login: self.login() }
					)();
			}
			return 'Not logged in';
		});
		self.authProviders = appConfig.authProviders;
		self.loginPlaceholder = ko.observable();
		self.passwordPlaceholder = ko.observable();

		self.getAuthProvider = (name) =>
			self.authProviders.filter((ap) => ap.name === name)[0];

		self.toggleCredentialsForm = function (provider) {
			self.isDbLoginAtt(!self.isDbLoginAtt());
			if (self.isDbLoginAtt()) {
				self.loginPlaceholder(
					provider ? provider.loginPlaceholder : null
				);
				self.passwordPlaceholder(
					provider ? provider.passwordPlaceholder : null
				);
			}
		};

		self.oauthSignIn = function (loginUrl) {
			const parts = window.location.href.split('#');
			let baseUrl = parts[0];
			if (baseUrl.endsWith('/')) {
				baseUrl = baseUrl.slice(0, -1);
			}
			let specificPage = '';
			if (parts.length === 2) {
				specificPage = parts[1];
				if (specificPage.startsWith('/')) {
					specificPage = specificPage.slice(1);
				}
			}
			const redirectUrl = encodeURIComponent(`${baseUrl}/#/welcome/OpenID/${specificPage}`);
			const oauthUrl = encodeURI(loginUrl + '?redirectUrl=' + redirectUrl);
			document.location = oauthUrl;
		};

		self.onLoginSuccessful = function (data) {
			sharedState.resetCurrentDataSourceScope();
			self.setAuthParams(
				data?.token,
				data?.permissions
			);
			self.loadUserInfo().then(() => {
				self.errorMsg(null);
				self.isBadCredentials(null);
				self.isInProgress(false);
			});
		};

		self.onLoginFailed = function (request, defaultMessage) {
			self.isInProgress(false);
			self.resetAuthParams();
			self.isBadCredentials(true);
			const msg = request.headers.get('x-auth-error');
			self.errorMsg(msg || defaultMessage);
		};

		self.signinWithLoginPass = function (data) {
			self.isInProgress(true);
			const url = self.serviceUrl + self.authUrl();
			const payload = {
				login: data.elements.lg_username.value,
				password: data.elements.lg_password.value,
			};
			httpService.doPost(url, payload).then(
				({ data }) => self.onLoginSuccessful(data),
				(response) =>
					self.onLoginFailed(
						response,
						ko.i18n(
							'components.welcome.messages.badCredentials',
							'Bad credentials'
						)()
					)
			);
		},

		self.signin = function (name) {
			const selectedProvider = self.getAuthProvider(name);
			if (selectedProvider.isUseCredentialsForm) {
				self.authUrl(selectedProvider.url);
				self.toggleCredentialsForm(selectedProvider);
			} else {
				const loginUrl = self.serviceUrl + selectedProvider.url;

				if (selectedProvider.ajax == true) {
					self.isInProgress(true);
					httpService.doGet(loginUrl).then(({ data }) => {
						self.onLoginSuccessful(data);
					}, (response) => {
						self.onLoginFailed(
							response,
							ko.i18n(
								'components.welcome.messages.loginFailed',
								'Login failed'
							)()
						);
					});
				} else {
					self.oauthSignIn(loginUrl);
				}
			}
		};

		if (self.authProviders.length === 1 && !self.isLoggedIn()) {
			self.signin(self.authProviders[0].name);
		}

		self.signout = function () {
			self.isInProgress(true);
			if (authApi.authClient() === authApi.AUTH_CLIENTS.SAML) {
				const id = 'saml-iframe';
				const iframe = `<iframe id="${id}" src="${
					self.serviceUrl + 'saml/slo'
				}" style="position: absolute; width:0;height:0;border:0; border:none;"></iframe>`;
				$('#' + id).remove();
				$('body').append(iframe);
			}
			httpService.doPost(self.serviceUrl + 'user/logout').then(() => {
				self.resetAuthParams();
			}, (error) => {
				if (error.status === 401) {
					self.resetAuthParams();
				}
			}).finally(() => {
				self.errorMsg(null);
				self.isInProgress(false);
			});
		};

		self.runAs = function () {
			self.isInProgress(true);
			authApi.runAs(
				self.runAsLogin(),
				self.onLoginSuccessful,
				(response) => {
					const msg = response.headers.get('x-auth-error');
					self.isInProgress(false);
					self.errorMsg(msg || 'User was not found');
				}
			);
		};

		self.signoutIap = function () {
			window.location = '/_gcp_iap/clear_login_cookie';
		};

		self.refreshPage = function () {
			window.location.reload();
		};
	}

	var component = {
		viewModel: welcome,
		template: view,
	};

	ko.components.register(componentName, component);
	return component;
});

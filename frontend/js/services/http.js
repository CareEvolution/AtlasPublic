define(function (require, exports) {
	const ko = require('knockout');
	const config = require('appConfig');
	const sharedState = require('atlas-state');
	const JSON_RESPONSE_TYPE = 'application/json';
	const TEXT_RESPONSE_TYPE = 'text/plain';
	const EventBus = require('services/EventBus');
	const STATUS = {
		OK: 200,
		UNAUTHORIZED: 401,
		FORBIDDEN: 403,
		NOT_FOUND: 404,
	};
	const METHODS = {
		GET: 'GET',
		POST: 'POST',
		PUT: 'PUT',
		DELETE: 'DELETE',
	};

	class Api {
		// TODO: Right now everyone is sending the full URL including the webAPIRoot
		//apiBaseUrl = config.webAPIRoot;
		apiBaseUrl = '';

		doGet(path, queryParams) {

			// TODO: Right now everyone is sending the full URL including the webAPIRoot
			// If we fix that, we'll need to adjust this to set path to just the path + query params
			if (queryParams) {
				const url = new URL(path, config.webAPIRoot);
				Object.keys(queryParams).forEach(key => {
					if (queryParams[key] !== null && queryParams[key] !== undefined) {
						url.searchParams.set(key, queryParams[key]);
					}
				});
				path = url.href;
			}

			return this.sendRequest(METHODS.GET, path);
		}

		doPost(path, payload, contentType) {
			// No one appears to be sending a function
			// There's one case in Vocabulary.js where data is sent as text and is returned as plain text (not JSON)
			return this.sendRequest(METHODS.POST, path, payload, contentType);
		}

		doPut(path, payload, contentType) {
			return this.sendRequest(METHODS.PUT, path, payload, contentType);
		}

		doDelete(path, payload, contentType) {
			return this.sendRequest(METHODS.DELETE, path, payload, contentType);
		}

		// Private
		sendRequest(method, path, payload, contentType) {
			const _this = this;

			const params = {
				method: method,
				headers: this.getHeaders(path),
			};

			if (this.credentialsRequired) {
				params.credentials = 'include';
			}

			if (payload && payload instanceof FormData) {
				params.body = payload;
				// NOTE:
				// Do not set 'Content-Type' - browser will automatically do this.
				// Problem is in a 'boundary'.
				// http://stackoverflow.com/questions/39280438/fetch-missing-boundary-in-multipart-form-data-post
			} else if (payload) {
				params.body = ko.toJSON(payload);
				if (contentType) {
					params.headers['Content-Type'] = contentType;
				} else {
					params.headers['Content-Type'] = JSON_RESPONSE_TYPE;
				}
			}

			var fullpath = this.apiBaseUrl + path;

			return fetch(fullpath, params)
				.then(function (res) {

					if (res.status >= 300 ) {
						return Promise.resolve(res);
					}

					return res
						.text()
						.then(function (text) {
							return res.headers.get('Content-Type') ===
								TEXT_RESPONSE_TYPE
								? text
								: _this.parseResponse(text);
						})
						.then(function (data) {
							return _this.sendResult(res, data);
						});
				})
				.then(function (res) {
					return _this.afterRequestHook(res, method, path);
				});
		}

		// Private
		parseResponse(text) {
			// Protection from empty response
			if (!text) {
				return {};
			}
			if (text === 'true') {
				return true;
			}
			if (text === 'false') {
				return false;
			}
			return JSON.parse(text);
		}

		// Private
		sendResult(res, parsedResponse) {
			return {
				ok: res.ok,
				status: res.status,
				json: parsedResponse,
				data: parsedResponse,
				headers: res.headers,
			};
		}

		// Private
		afterRequestHook(res) {
			if (!this.checkStatusError(res)) {
				throw res;
			}
			return res;
		}

		// Private
		handleForbiddenError() {
			console.error('Access is forbidden. Login required.');
		}

		// Private
		handleUnexpectedError() {
			console.error('Oooops!.. Something went wrong :(');
		}

		// Private
		handleNotFoundError(responseJson) {
			if (
				responseJson !== undefined &&
				responseJson.payload !== undefined
			) {
				EventBus.errorMsg(responseJson.payload.message);
			}
		}

		setUnauthorizedHandler(handler) {
			this.handleUnauthorized = handler;
			return this;
		}

		// Private
		handleUnauthorized(responseJson) {}

		// Private
		checkStatusError(response) {
			const status = response.status;

			if (status >= 200 && status < 300) {
				return true;
			}

			switch (status) {
				case STATUS.NOT_FOUND:
					this.handleNotFoundError(response.json);
					break;
				case STATUS.UNAUTHORIZED:
					this.handleUnauthorized(response.json);
					break;
				case STATUS.FORBIDDEN:
					this.handleForbiddenError();
					break;
				default:
					this.handleUnexpectedError();
					break;
			}

			return false;
		}

		getXsrfToken() {
			const xsrfCookie = document.cookie
				.split('; ')
				.find((row) => row.startsWith('XSRF-TOKEN='));
			return xsrfCookie ? xsrfCookie.split('=')[1] : null;
		}

		// Private
		getHeaders(requestUrl) {
			const headers = {
				'User-Language': ko.unwrap(sharedState.locale),
			};

			headers['Action-Location'] = location;

			const xsrfToken = this.getXsrfToken();
			if (xsrfToken) {
				headers['X-XSRF-TOKEN'] = xsrfToken;
			}

			return headers;
		}

		// Private
		credentialsRequired() {
			return config.authProviders.some((provider) => provider.withCredentials);
		}
	}

	const singletonApi = new Api();
	return singletonApi;
});

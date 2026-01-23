define([
	'pages',
	'./post-auth-routes',
	'querystring',
	'services/AuthAPI',
	'atlas-state',
	'knockout',
	'const',
	'services/EventBus',
	'director',
], (
	pages,
	postAuthRoutes,
	querystring,
	authApi,
	sharedState,
	ko,
	constants,
	EventBus
) => {
	class AtlasRouter {
		constructor() {
			this.defaultRoute = '/datasources';
			this.activeRoute = ko.observable({});
			this.currentView = ko.observable('loading');
			this.onLoginSubscription;
			this.pages = Object.values(pages);
			this.routerParams = ko.observable();
			this.currentViewAccessible = ko.pureComputed(() => {
				return (
					this.currentView &&
					sharedState.appInitializationStatus() !==
						constants.applicationStatuses.failed &&
					(sharedState.appInitializationStatus() !==
						constants.applicationStatuses.noSourcesAvailable ||
						['ohdsi-configuration', 'source-manager'].includes(
							this.currentView()
						))
				);
			});
			this.currentView.subscribe(() => {
				EventBus.errorMsg(undefined);
			});
		}

		run() {
			const routerOptions = {
				notfound: () => this.handleNotFound(),
			};
			this.router = new Router(this.aggregateRoutes());
			this.router.qs = this.qs;
			this.router.configure(routerOptions);
			this.router.init(this.defaultRoute);
		}

		qs() {
			return querystring.parse(window.location.href.split('?')[1]);
		}

		handleNotFound() {
			this.router.setRoute(this.defaultRoute);
		}

		aggregateRoutes() {
			let routes = this.pages.reduce((routes, page) => {
				const pageRoutes = page.buildRoutes(this);
				for (const key in pageRoutes) {
					pageRoutes[key].title = page.title;
				}
				return {
					...routes,
					...pageRoutes,
				};
			}, {});
			const postAuthRoutesObj = postAuthRoutes(this);
			for (const key in postAuthRoutesObj) {
				postAuthRoutesObj[key].title = 'Post Auth';
			}
			routes = {
				...routes,
				...postAuthRoutesObj,
			};

			const routesWithRefreshedToken = Object.keys(routes).reduce(
				(accumulator, key) => {
					accumulator[key] = (...args) => {
						sharedState.loading(true);
						if (this.onLoginSubscription) {
							this.onLoginSubscription.dispose();
						}
						const handler = routes[key].handler.bind(null, ...args);
						const title = routes[key].title;
						routes[key]
							.checkPermission()
							.then(() => handler())
							.catch((ex) => {
								console.error(
									ex !== undefined ? ex : 'Permission error'
								);
								// protected route didn't pass the token check -> show white page
								this.setCurrentView('white-page');
								// wait until user authenticates
								this.schedulePageUpdateOnLogin(handler);
							})
							.finally(() => {
								sharedState.loading(false);
							});

						this.activeRoute({
							handler,
							isSecured: routes[key].isSecured,
							title: routes[key].title,
						});
					};
					return accumulator;
				},
				{}
			);
			// anyway, we should track the moment when the user exits and check permissions once again
			authApi.isAuthenticated.subscribe((isAuthenticated) => {
				const { isSecured, handler } = this.activeRoute();
				if (!isAuthenticated && isSecured) {
					this.setCurrentView('white-page');
					this.schedulePageUpdateOnLogin(handler);
				}
			});

			return routesWithRefreshedToken;
		}

		schedulePageUpdateOnLogin(routeHandler) {
			this.onLoginSubscription = authApi.isAuthenticated.subscribe(
				(isAuthenticated) => {
					if (isAuthenticated) {
						routeHandler();
						this.onLoginSubscription.dispose();
					}
				}
			);
		}

		setCurrentView(view, routerParams = false) {
			if (view !== this.currentView()) {
				this.currentView('loading');
			}
			if (routerParams !== false) {
				this.routerParams(routerParams);
			}
			this.currentView(view);
		}
	}
	return new AtlasRouter();
});

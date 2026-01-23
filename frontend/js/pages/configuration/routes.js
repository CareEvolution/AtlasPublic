define((require, factory) => {
	const atlasState = require('atlas-state');
	const { AuthorizedRoute } = require('pages/Route');
	function routes(router) {
		const JobViewEdit = new AuthorizedRoute((id, section) => {
			require(['./users-import/job-view-edit'], function () {
				router.setCurrentView('import-job-view-edit', {
					jobId: id,
					section: section,
				});
			});
		});
		return {
			'/configure': new AuthorizedRoute(() => {
				require([
					'./configuration',
				], function () {
					router.setCurrentView('ohdsi-configuration');
				});
			}),
			'/role/:id': new AuthorizedRoute((id) => {
				require(['./roles/role-details'], function () {
					const roleId = parseInt(id);
					router.setCurrentView('role-details', { roleId });
				});
			}),
			import: new AuthorizedRoute(() => {
				require(['./users-import/browser'], function () {
					router.setCurrentView('user-import-browser');
				});
			}),
			'import/job/:id:': JobViewEdit,
			'import/job/:id:/:section:': JobViewEdit,
			'import/wizard': new AuthorizedRoute(() => {
				require(['./users-import/users-import'], function () {
					router.setCurrentView('users-import');
				});
			}),
			'import/roles': new AuthorizedRoute(() => {
				require(['./roles/role-import'], function () {
					router.setCurrentView('role-import');
				});
			}),
			'/source/:id': new AuthorizedRoute((id) => {
				const sourceId = parseInt(id);
				require(['./sources/source-manager'], function () {
					atlasState.ConfigurationSource.selectedId(sourceId);
					router.setCurrentView('source-manager', { sourceId });
				});
			}),
			'/configure/tag-management': new AuthorizedRoute(() => {
				require([
					'./configuration',
					'./tag-management/tag-management',
				], function () {
					router.setCurrentView('ohdsi-configuration', {
						section: 'tag-management',
						component: 'tag-management',
					});
				});
			}),
			'/configure/sources': new AuthorizedRoute(() => {
				require([
					'./configuration',
					'./sources/source-list',
				], function () {
					router.setCurrentView('ohdsi-configuration', {
						section: 'sources',
						component: 'source-list',
					});
				});
			}),
			'/configure/roles': new AuthorizedRoute(() => {
				require(['./configuration', './roles/roles'], function () {
					router.setCurrentView('ohdsi-configuration', {
						section: 'roles',
						component: 'roles',
					});
				});
			}),
		};
	}

	return routes;
});

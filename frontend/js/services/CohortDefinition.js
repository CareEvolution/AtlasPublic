define(['require'], function (require) {

	const config = require('appConfig');
	const authApi = require('services/AuthAPI');
	const httpService = require('services/http');

	function getCohortDefinitionList() {
		return httpService
			.doGet(config.webAPIRoot + 'cohortdefinition')
			.then(({ data }) => data);
	}

	async function saveCohortDefinition(definition) {
		const url =
			config.webAPIRoot +
			'cohortdefinition' +
			(definition.id ? '/' + definition.id : '');;
		const promise = definition.id
			? httpService.doPut(url, definition).then(({ data }) => data)
			: httpService.doPost(url, definition).then(({ data }) => data);
		return authApi.executeWithRefresh(promise);
	}

	async function copyCohortDefinition(id) {
		const promise = httpService
			.doGet(
				config.webAPIRoot + 'cohortdefinition/' + (id || '') + '/copy'
			)
			.then(({ data }) => data);
		return authApi.executeWithRefresh(promise);
	}

	function deleteCohortDefinition(id) {
		return httpService
			.doDelete(config.webAPIRoot + 'cohortdefinition/' + (id || ''))
			.then(({ data }) => data);
	}

	async function getCohortDefinition(id) {
		return authApi.executeWithRefresh(httpService
			.doGet(config.webAPIRoot + 'cohortdefinition/' + id)
			.then(({data}) => data));
	}

	function exists(name, id) {
		return httpService
			.doGet(`${config.webAPIRoot}cohortdefinition/${id}/exists?name=${name}`)
			.then(res => res.data);
	}

	function getSql(expression, options) {
		return httpService
			.doPost(config.webAPIRoot + 'cohortdefinition/sql', {
				expression: expression,
				options: options,
			})
			.then((res) => res.data);
	}

	function translateSql(sql, dialect) {
		return httpService
			.doPost(config.webAPIRoot + 'sqlrender/translate', {
				SQL: sql,
				targetdialect: dialect,
			})
			.catch((error) => console.log('Error: ' + error));
	}


	function generate(cohortDefinitionId, sourceKey) {
		return httpService.doGet(`${config.webAPIRoot}cohortdefinition/${cohortDefinitionId}/generate/${sourceKey}`);
	}


	function cancelGenerate(cohortDefinitionId, sourceKey) {
		return httpService.doGet(
			config.webAPIRoot +
				'cohortdefinition/' +
				(cohortDefinitionId || '-1') +
				'/cancel/' +
				sourceKey
		).then(res => res.data);
	}

	function getInfo(cohortDefinitionId) {
		return httpService
			.doGet(
				config.webAPIRoot +
					'cohortdefinition/' +
					(cohortDefinitionId || '-1') +
					'/info'
			)
			.then((res) => res.data);
	}

	function getReport(cohortDefinitionId, sourceKey, modeId) {
		return httpService.doGet(
			`${config.webAPIRoot}cohortdefinition/${
				cohortDefinitionId || '-1'
			}/report/${sourceKey}?mode=${modeId || 0}`
		)
		.then(res => res.data);
	}

	function runDiagnostics(expression) {
		return httpService.doPost(config.webAPIRoot + 'cohortdefinition/checkV2', expression)
			.then(res => res.data);
	}

	function getCohortCount(sourceKey, cohortDefinitionId) {
		return httpService.doGet(config.api.url + 'cohortresults/' + sourceKey + '/' + cohortDefinitionId + '/distinctPersonCount')
			.then(({ data }) => data);
	}

	function getCohortAnalyses(cohortJob) {
		return httpService.doPost(config.api.url + 'cohortanalysis', cohortJob);
	}

	function getCohortPrintFriendly(cohortExpression) {
		return httpService.doPost(
			config.webAPIRoot +
				'cohortdefinition/printfriendly/cohort?format=html',
				cohortExpression,
			).then(res => res.data.entity);
	}

	function getVersions(cohortDefinitionId) {
		return httpService.doGet(`${config.webAPIRoot}cohortdefinition/${cohortDefinitionId}/version`)
			.then(({ data }) => data);
	}

	function getVersion(cohortDefinitionId, versionNumber) {
		return httpService.doGet(`${config.webAPIRoot}cohortdefinition/${cohortDefinitionId}/version/${versionNumber}`)
			.then(res => {
				const cohortDef = res.data.entityDTO;
				cohortDef.expression = JSON.parse(cohortDef.expression);
				cohortDef.versionDef = res.data.versionDTO;
				return cohortDef;
			}).catch(error => {
				console.log("Error: " + error);
				authApi.handleAccessDenied(error);
			});
	}

	function copyVersion(cohortDefinitionId, versionNumber) {
		return authApi.executeWithRefresh(httpService
			.doPut(`${config.webAPIRoot}cohortdefinition/${cohortDefinitionId}/version/${versionNumber}/createAsset`)
			.then(({ data }) => data));
	}

	function updateVersion(version) {
		return httpService.doPut(`${config.webAPIRoot}cohortdefinition/${version.assetId}/version/${version.version}`, {
			comment: version.comment,
			archived: version.archived
		}).then(({ data }) => data);
	}

	var api = {
		getCohortDefinitionList,
		saveCohortDefinition,
		copyCohortDefinition,
		deleteCohortDefinition,
		getCohortDefinition,
		getSql,
		translateSql,
		generate,
		getInfo,
		getReport,
		runDiagnostics,
		cancelGenerate,
		getCohortCount,
		getCohortAnalyses,
		exists: exists,
		getCohortPrintFriendly,
		getVersions,
		getVersion,
		updateVersion,
		copyVersion
	};

	return api;
});

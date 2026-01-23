define(function (require) {

	const config = require('appConfig');
	const sharedState = require('atlas-state');
	const httpService = require('services/http');
	const CDMResultAPI = require('services/CDMResultsAPI');

	let domainsPromise = null;
	let domains = [];

	function getVocabUrl(url, sourceKey) {
		return sourceKey === undefined ? sharedState.vocabularyUrl() : (url || config.webAPIRoot) + 'vocabulary/' + sourceKey;
	}

	function encodeQuery(str) {
		str = encodeURIComponent(str);
		str = str.replace(/\*/g, '%2A'); // handle asterisk for wildcard search
		return str;
	}

	function getDomains() {
		// if domains haven't yet been requested, create the promise
		if (!domainsPromise) {
			if (sharedState.vocabularyUrl()) {
				domainsPromise = new Promise((resolve, reject) => {
					httpService
						.doGet(sharedState.vocabularyUrl() + 'domains')
						.then(({data}) => {
							data.forEach((v) => { domains.push(v.DOMAIN_ID); });
							resolve(domains);
						});
				});
			} else {
				return new Promise((resolve, reject) => resolve([]));
			}
		}
		return domainsPromise;
	}

	function loadDensity(results, sourceKey, formatter) {

		if (results.length == 0) {
			return Promise.resolve();
		}

		var searchResultIdentifiers = [];
		for (let c = 0; c < results.length; c++) {
			results[c].RECORD_COUNT = 0;
			results[c].DESCENDANT_RECORD_COUNT = 0;
			results[c].PERSON_COUNT = 0;
			results[c].DESCENDANT_PERSON_COUNT = 0;
			searchResultIdentifiers.push(results[c].CONCEPT_ID);
		}
		return sourceKey ?
			CDMResultAPI.getConceptRecordCount(sourceKey, searchResultIdentifiers, results, false, formatter) :
			CDMResultAPI.getConceptRecordCountWithResultsUrl(sharedState.resultsUrl(), searchResultIdentifiers, results, false, formatter);
	}

	async function search(params) {
		const vocabUrl = getVocabUrl();

		if (params.QUERY && Object.keys(params).length == 1)  // simple search via GET
		{
			return  httpService.doGet(`${vocabUrl}search?query=${encodeQuery(params.QUERY)}`).then(({ data }) => data);
		}

		return httpService.doPost(`${vocabUrl}search`,params).then(({ data }) => data);
	}

	function getConcept(id) {
		const vocabUrl = getVocabUrl();

		return httpService
			.get(vocabUrl + 'concept/' + id)
			.then(({ data }) => data);
	}

	function getConceptSetList(url) {
		var repositoryUrl;

		if (url)
			repositoryUrl = url + 'conceptset';
		else
			repositoryUrl = config.webAPIRoot + 'conceptset';

		return httpService
			.doGet(repositoryUrl)
			.then(({ data }) => data);
	}

	function getConceptSetExpression(id, url) {
		let repositoryUrl;

		if (url)
			repositoryUrl = url + 'conceptset/';
		else
			repositoryUrl = config.webAPIRoot + 'conceptset/';

		repositoryUrl += id + '/expression';

		return httpService.doGet(repositoryUrl).then(({ data }) => data);
	}

	function resolveConceptSetExpression(expression, url, sourceKey) {
		const vocabUrl = getVocabUrl(url, sourceKey);
		return httpService.doPost(vocabUrl + 'resolveConceptSetExpression', expression).then(({ data }) => data);
	}

	function getConceptSetExpressionSQL(expression, url) {
		const repositoryUrl = (url || config.webAPIRoot) + 'vocabulary/conceptSetExpressionSQL';

		return httpService.doPost(repositoryUrl, expression, 'text/plain');
	}

	function getConceptsById(identifiers, url, sourceKey) {
		const vocabUrl = getVocabUrl(url, sourceKey);
		const repositoryUrl = vocabUrl + 'lookup/identifiers';

		var getConceptsByIdPromise = httpService.doPost(repositoryUrl, identifiers);

		return getConceptsByIdPromise;
	}

	function getConceptsByCode(codes) {
		var url = sharedState.vocabularyUrl() + 'lookup/sourcecodes';
		return httpService.doPost(url, codes);
	}

	function getMappedConceptsById(identifiers, url, sourceKey) {
		const vocabUrl = getVocabUrl(url, sourceKey);
		return httpService.doPost(vocabUrl + 'lookup/mapped', identifiers).then(({ data }) => data);
	}

	function getRecommendedConceptsById(identifiers, url, sourceKey) {
		const vocabUrl = getVocabUrl(url, sourceKey) + 'lookup/recommended';
		return httpService.doPost(vocabUrl, identifiers).then(({ data }) => data);
	}

	function optimizeConceptSet(conceptSetItems, url, sourceKey) {
		const vocabUrl = getVocabUrl(url, sourceKey);
		return httpService
			.doPost(vocabUrl + 'optimize', conceptSetItems)
			.then(({ data }) => data);
	}

	function compareConceptSet(compareTargets, url, sourceKey) {
		const vocabUrl = getVocabUrl(url, sourceKey);
		return httpService
			.doPost(vocabUrl + 'compare', compareTargets)
			.then(({ data }) => data);
	}

	function compareConceptSetCsv(compareTargets,types, url, sourceKey) {
		const vocabUrl = getVocabUrl(url, sourceKey);
		return httpService
			.doPost(vocabUrl + 'compare', {
				compareTargets: compareTargets,
				types: types,
			})
			.then(({ data }) => data);
	}
	
	async function loadAncestors(ancestors, descendants, url, sourceKey) {
		const vocabUrl = getVocabUrl(url, sourceKey);
		const data = { ancestors, descendants };
		return httpService.doPost(vocabUrl + 'lookup/identifiers/ancestors', data);
	}	

	var api = {
		search: search,
		getDomains: getDomains,
		getConcept: getConcept,
		getConceptSetList: getConceptSetList,
		getConceptSetExpression: getConceptSetExpression,
		resolveConceptSetExpression: resolveConceptSetExpression,
		getConceptsById: getConceptsById,
		getConceptsByCode: getConceptsByCode,
		getMappedConceptsById: getMappedConceptsById,
		getRecommendedConceptsById: getRecommendedConceptsById,
		getConceptSetExpressionSQL: getConceptSetExpressionSQL,
		optimizeConceptSet: optimizeConceptSet,
		compareConceptSet: compareConceptSet,
		compareConceptSetCsv: compareConceptSetCsv,
		loadDensity: loadDensity,
		loadAncestors,
	};

	return api;
});

define(function (require, exports) {
	const httpService = require('services/http');
	const config = require('appConfig');

	function getNegativeControls(sourceKey, conceptSetId) {
		return httpService
			.doGet(
				config.webAPIRoot +
					'evidence/' +
					sourceKey +
					'/negativecontrols/' +
					(conceptSetId || '-1')
			)
			.then(({ data }) => data);
	}

	function getDrugLabelExists(sourceKey, conceptIds) {
		return httpService
			.doPost(
				config.webAPIRoot + 'evidence/' + sourceKey + '/druglabel',
				conceptIds
			)
			.then(({ data }) => data);
	}

	function getDrugConditionPairs(
		sourceKey,
		targetDomainId,
		drugConceptIds,
		conditionConceptIds,
		sourceIds
	) {
		return httpService
			.doPost(
				config.webAPIRoot +
					'evidence/' +
					sourceKey +
					'/drugconditionpairs',
				{
					targetDomain: targetDomainId,
					drugConceptIds: drugConceptIds,
					conditionConceptIds: conditionConceptIds,
					sourceIds: sourceIds,
				}
			)
			.then(({ data }) => data);
	}

	function generateNegativeControls(
		sourceKey,
		conceptSetId,
		conceptSetName,
		conceptDomainId,
		targetDomainId,
		conceptIds,
		csToInclude,
		csToExclude
	) {
		return httpService
			.doPost(
				config.webAPIRoot +
					'evidence/' +
					sourceKey +
					'/negativecontrols',
				{
					jobName: 'NEGATIVE_CONTROLS_' + conceptSetId,
					conceptSetId: conceptSetId,
					conceptSetName: conceptSetName,
					conceptDomainId: conceptDomainId,
					outcomeOfInterest: targetDomainId,
					conceptsOfInterest: conceptIds,
					csToInclude: csToInclude,
					csToExclude: csToExclude,
					//translatedSchema: "translated",
				}
			)
			.then(({ data }) => data);
	}

	var api = {
		getDrugConditionPairs: getDrugConditionPairs,
		getDrugLabelExists: getDrugLabelExists,
		getNegativeControls: getNegativeControls,
		generateNegativeControls: generateNegativeControls,
	};

	return api;
});

define(['knockout', 'services/AuthAPI'], function (ko, AuthAPI) {
	const canCreateCC = ko.observable(false);
	AuthAPI.canCreateCohortCharacterization().then((canCreate) => {
		canCreateCC(canCreate);
	});

	function isPermittedGetCCList() {
		return true;
	}

	function isPermittedGetCC(id) {
		return true;
	}

	function isPermittedUpdateCC(design) {
		return design?.hasWriteAccess || false;
	}

	function isPermittedDeleteCC(design) {
		return design?.hasDeleteAccess || false;
	}

	function isPermittedListGenerations(id) {
		return true;
	}

	function isPermittedGenerate(design, sourceKey) {
		return design?.hasWriteAccess || false;
	}

	function isPermittedResults(sourceKey) {
		return true;
	}

	function isPermittedExportGenerationDesign(id) {
		return true;
	}

	function isPermittedExportCC(id) {
		return true;
	}

	//

	const canCreateFa = ko.observable(false);
	AuthAPI.canCreateFeatureAnalysis().then((canCreate) => {
		canCreateFa(canCreate);
	});

	function isPermittedGetFaList() {
		return true;
	}

	function isPermittedGetFa(id) {
		return true;
	}

	function isPermittedUpdateFa(fa) {
		return fa?.hasWriteAccess || false;
	}

	function isPermittedDeleteFa(fa) {
		return fa?.hasDeleteAccess || false;
	}

	return {
		canCreateCC,
		isPermittedGetCCList,
		isPermittedGetCC,
		isPermittedUpdateCC,
		isPermittedDeleteCC,
		isPermittedListGenerations,
		isPermittedGenerate,
		isPermittedResults,
		isPermittedExportGenerationDesign,
		isPermittedExportCC,
		//
		canCreateFa,
		isPermittedGetFaList,
		isPermittedGetFa,
		isPermittedUpdateFa,
		isPermittedDeleteFa,
	};
});

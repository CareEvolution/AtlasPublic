define([
	'knockout',
	'components/Component',
	'utils/AutoBind',
	'components/cohortbuilder/CohortDefinition',
	'services/CohortDefinition',
	'utils/CommonUtils',
	'text!./cohort-definition-importer.html',
], function (
	ko,
	Component,
	AutoBind,
	CohortDefinition,
	cohortDefinitionService,
	commonUtils,
	view
) {
	class CohortDefinitionImporter extends AutoBind(Component) {
		constructor(params) {
			super(params);

			this.dialogIsOpen = params.dialogIsOpen;;
			this.cohortDefinitionId = params.cohortDefinitionId;

			this.cohortDefinitionJson = ko.observable();
			this.cohortDefinitionName = ko.observable();
			this.cohortDefinitionDescription = ko.observable();
			this.isSaving = ko.observable(false);
			this.isError = ko.observable(false);
		}

		importDisabled = ko.pureComputed(() => {
			return !this.cohortDefinitionJson() || !this.cohortDefinitionName() || this.isSaving();
		});

		loadExpression(selectedFile) {
			const reader = new FileReader();

			reader.onload = (e) => {
				const fileContent = e.target.result;
				this.cohortDefinitionJson(fileContent);
			};

			reader.onerror = (e) => {
				console.error('Error reading file:', e.target.error);
			};

			reader.readAsText(selectedFile);
		}

		async importCohortDefinition() {
			if (!this.cohortDefinitionJson() || !this.cohortDefinitionName()) {
				return;
			}

			try {
				this.isSaving(true);
				this.isError(false);

				const cohortDefinition = new CohortDefinition({
					id: undefined,
					name: this.cohortDefinitionName().trim(),
					description: this.cohortDefinitionDescription(),
					expression: JSON.parse(this.cohortDefinitionJson()),
				});
				const cohortDefinitionJson = ko.toJS(cohortDefinition);

				const savedCohortDefinition = await cohortDefinitionService.saveCohortDefinition(
					cohortDefinitionJson
				);

				this.cohortDefinitionId(savedCohortDefinition.id);
				this.dialogIsOpen(false);
			} catch (e) {
				console.error(e);
				this.isError(true);
			} finally {
				this.isSaving(false);
			}
		}

		close() {
			this.dialogIsOpen(false);
		}
	}

	return commonUtils.build(
		'cohort-definition-importer',
		CohortDefinitionImporter,
		view
	);
});

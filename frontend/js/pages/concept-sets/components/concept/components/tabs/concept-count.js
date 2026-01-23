define([
	'knockout',
	'text!./concept-count.html',
	'components/Component',
	'utils/CommonUtils',
	'atlas-state',
	'services/http',
	'services/DatatableLayoutService',
], function (
	ko,
	view,
	Component,
	commonUtils,
	sharedState,
	httpService,
	layoutService,
) {
	class ConceptManager extends Component {
		constructor(params) {
			super(params);
			this.currentConceptId = params.currentConceptId;
			this.hasInfoAccess = params.hasInfoAccess;
			this.isAuthenticated = params.isAuthenticated;
			this.tableOptions = commonUtils.getTableOptions('L');
			this.sourceCounts = ko.observableArray();
			this.isLoading = ko.observable(false);
			this.layout = layoutService.tableOnly;

			this.loadRecordCounts();
		}

		async fetchRecordCounts(sources) {
			const promises = [];
			const sourceData = [];
			for (const source of sources) {
				const { sourceName, sourceKey, resultsUrl } = source;
				// await is harmless here since it will pull data sequentially while it can be done in parallel
				const promise = httpService
					.doPost(`${resultsUrl}conceptRecordCount`, [
						this.currentConceptId(),
					])
					.then(({ data }) => {
						const recordCountObject =
							data.length > 0 ? Object.values(data[0])[0] : null;
						if (recordCountObject) {
							sourceData.push({
								sourceName,
								recordCount: recordCountObject[0],
								descendantRecordCount: recordCountObject[1],
							});
						}
					})
					.catch((err) => {
						const failedMsg = ko.i18n(
							'cs.manager.concept.tabs.recordCounts.failedToLoadData',
							'Failed to load data'
						);
						sourceData.push({
							sourceName,
							recordCount: failedMsg,
							descendantRecordCount: failedMsg,
						});
					});
				promises.push(promise);
			}
			
			// Promise.allSettled works since Chrome v76 so we need polyfill for it
			await Promise.allSettled(promises);
			return sourceData;
		}

		async loadRecordCounts() {
			this.isLoading(true);
			try {
					const sourcesWithResults = sharedState.sources().filter(source => source.hasResults);
					const sourceData = await this.fetchRecordCounts(sourcesWithResults);
					this.sourceCounts(sourceData);
			} finally {
					this.isLoading(false);
			}
		}
	}

	return commonUtils.build('concept-count', ConceptManager, view);
});

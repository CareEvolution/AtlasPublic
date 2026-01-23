define([
	'knockout',
	'text!./conceptsets-list.html',
	'components/Component',
	'utils/AutoBind',
	'utils/CommonUtils',
	'services/AuthAPI',
	'components/conceptset/ConceptSetStore',
	'../const',
	'components/tabs',
	'circe'
], function (
	ko,
	view,
	Component,
	AutoBind,
	commonUtils,
	authApi,
	ConceptSetStore,
	constants,
) {
	class ConceptsetList extends AutoBind(Component) {
		constructor(params) {
			super(params);
			this.conceptSetStore = ConceptSetStore.getStore(ConceptSetStore.sourceKeys().repository);
			this.currentConceptSet = this.conceptSetStore.current;
			this.canCreateConceptSet = ko.observable(false);
			authApi.canCreateConceptSet().then((canCreate) => {
				this.canCreateConceptSet(canCreate);
			});
			this.tableOptions = commonUtils.getTableOptions('L');
		}

		onRespositoryConceptSetSelected (conceptSet) {
			commonUtils.routeTo(constants.paths.mode(conceptSet.id));
		}

		onConceptSetBrowserAction (result) {
			// Inspect the result to see what type of action was taken. For now
			// we're handling the 'add' action
			if (result.action == 'add') {
				this.newConceptSet();
			}
		}

		newConceptSet() {
			if (this.currentConceptSet() == undefined) {
				commonUtils.routeTo(constants.paths.mode());
			}
		}

	}

	return commonUtils.build('conceptsets-list', ConceptsetList, view);
});
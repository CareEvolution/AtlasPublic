define([
	'knockout',
	'text!./vocabulary.html',
	'pages/Page',
	'utils/CommonUtils',
	'components/heading',
	'./components/search',
	'less!./vocabulary.less',
], function (ko, view, Page, commonUtils) {
	class Vocabulary extends Page {
		constructor(params) {
			super(params);
			this.model = params.model;
			this.query = ko.observable();
		}

		onRouterParamsChanged({ query }) {
			if (query !== undefined) {
				this.query(query);
			}
		}
	}

	return commonUtils.build('vocabulary', Vocabulary, view);
});

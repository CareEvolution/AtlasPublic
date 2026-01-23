define(function (require, exports) {
	
	var ko = require('knockout');
		
	var conceptSetBrowser = require('./components/ConceptSetBrowser');
	ko.components.register('concept-set-browser', conceptSetBrowser);
	
});

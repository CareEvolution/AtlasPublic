define(['knockout'], function (ko) {
	ko.bindingHandlers.title = {
		init: function (element, valueAccessor) {
			ko.applyBindingsToNode(element, {
				attr: { title: valueAccessor() },
			});
		},
		update: function (element, valueAccessor) {
			ko.applyBindingsToNode(element, {
				attr: { title: valueAccessor() },
			});
		},
	};
});

define(['knockout', 'jquery', 'bootstrap'], function (ko, $, bootstrap) {
	ko.bindingHandlers.tooltip = {
		init: function (element, valueAccessor) {
			const value = ko.utils.unwrapObservable(valueAccessor());
			$(element).attr('title', value);
			const tooltip = new bootstrap.Tooltip(element, {
				html: true,
				container: 'body',
			});

			ko.utils.domNodeDisposal.addDisposeCallback(element, function () {
				tooltip.dispose();
			});
			
		},
		update: function (element, valueAccessor) {
			const value = ko.utils.unwrapObservable(valueAccessor());
			$(element).attr('data-original-title', value);
		},
	};
});

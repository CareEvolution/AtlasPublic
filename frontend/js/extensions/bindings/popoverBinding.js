define(['knockout', 'jquery', 'bootstrap'], function (ko, $, bootstrap) {
	ko.bindingHandlers.popover = {
		init: function (element, valueAccessor) {
			let popover = undefined;
			const value = ko.utils.unwrapObservable(valueAccessor());
			const popoverOptions = {
				html: true,
				container: 'body',
				trigger: value.trigger || 'hover',
				placement: value.placement || 'auto',
				customClass: value.customClass || ''
			};
			if (typeof value === 'string') {
				popover = new bootstrap.Popover(element, {
					content: value,
					...popoverOptions,
				});
			} else if (typeof value === 'object') {
				popover = new bootstrap.Popover(element, {
					content: () => {
						return value.template.cloneNode(true);
					},
					...popoverOptions,
				});

				element.addEventListener('shown.bs.popover', function () {
					ko.cleanNode(popover.tip);
					ko.applyBindings(value.viewModel, popover.tip);
				});
			} else {
				throw new Error('Invalid popover value type');
			}

			ko.utils.domNodeDisposal.addDisposeCallback(element, function () {
				popover.dispose();
			});
		},
	};
});

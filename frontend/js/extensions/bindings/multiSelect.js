define([
	'knockout',
	'slim-select',
	'jquery'
], 
function (
	ko, 
	SlimSelect, 
	$
) {
	ko.bindingHandlers.multiSelect = {
		init: function (element, valueAccessor) {

			const data = valueAccessor();

			const options = {
				select: $(element)[0],
				data: data.options() || [],
				settings: {
					showSearch: data.liveSearch,
					closeOnSelect: data.closeOnSelect,
					maxValuesShown: 2,
					allowDeselect: true,
				},
				events: {
					afterChange: data.afterChange || (() => {}),
				},
			};

			const select = new SlimSelect(options);

			if (data.options) {
				data.options.subscribe(() =>
					select.setData(data.options())
				);
			}

			ko.utils.domNodeDisposal.addDisposeCallback(element, function () {
				select.destroy();
			});
		},
	};
});

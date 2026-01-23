define([
	'knockout',
	'text!./multi-select.html',
	'lodash',
	'less!./multi-select.less',
	'extensions/bindings/multiSelect',
	'databindings/eventListenerBinding',
], function (ko, view, _) {
	function multiSelectFilter(params) {
		var self = this;

		self.multiple = params.multiple;
		self.disabled = params.disable || false;

		// Prepend localOptions with a placeholder option copilot!
		self.options = ko.computed(() => {
			return [
				{
					placeholder: true,
					text: params.placeholder || 'Select an option...',
				},
				...params.options().map((opt) => {
					return {
						text: opt.label,
						value: opt.value,
						selected:
						(params.selectedValue &&
							params.selectedValue() === opt.value) ||
							(params.selectedValues &&
								params.selectedValues().includes(opt.value)),
							};
						}),
			];
		});

		self.onMultiSelectionComplete = function (data) {
			// only reset the param's selectedValues if the current selections are different
			const values = data.map((item) => item.value);
			if (
				params.multiple &&
				!_.xor(params.selectedValues(), values).length == 0
			) {
				params.selectedValues(values);
			}
		};

		self.onSingleSelectionComplete = function (data) {
			if (data.length > 0) {
				const value = data[0].value;
				if (params.selectedValue() !== value) {
					params.selectedValue(value);
				}
			} else {
				params.selectedValue(null);
			}
		};
	}

	const component = {
		viewModel: multiSelectFilter,
		template: view,
	};

	ko.components.register('multi-select', component);

	return component;
});

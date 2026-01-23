define(['knockout', 'jquery', 'flatpickr'], function(ko, $) {
	ko.bindingHandlers.dateTimePicker = {
		init: function (element, valueAccessor, allBindingsAccessor) {
			//initialize datepicker with some optional options
			$(element).flatpickr({
				enableTime: true,
				dateFormat: 'Y-m-d H:i',
				onChange: function (selectedDates, dateStr, instance) {
					//when a user changes the date, update the view model
					const value = valueAccessor();
					if (ko.isObservable(value)) {
						if (selectedDates.length > 0) value(selectedDates[0]);
					} else {
						value(null);
					}
				},
			});

			ko.utils.domNodeDisposal.addDisposeCallback(element, function () {
				const picker = element._flatpickr;
				if (picker) {
					picker.destroy();
				}
			});
		},
		update: function (element, valueAccessor, allBindings, viewModel, bindingContext) {

			const picker = element._flatpickr;
			//when the view model is updated, update the widget
			if (picker) {
				let koDate = ko.utils.unwrapObservable(valueAccessor());

				//in case return from server datetime i am get in this form for example /Date(93989393)/ then fomat this
				koDate = (typeof (koDate) !== 'object') ? new Date(parseFloat(koDate)) : koDate;

				picker.setDate(koDate);
			}
		}
	};
});
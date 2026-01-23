define([
	'knockout',
], (
	ko,
) => {

	const renderCheckbox = (field, clickable = true) => {
		return `<span data-bind="${clickable ? `click: function(d) { d.${field}(!d.${field}()); },` : ''} css: { selected: ${field} }" class="fa fa-check custom-check-box"></span>`;
	};

	const renderConceptSetCheckbox = (hasPermissions, field, readonly = false) => {
		return ko.unwrap(hasPermissions) && !readonly
			? `<span data-bind="click: function(d) { d.${field}(!d.${field}()); }, css: { selected: ${field} }" class="fa fa-check custom-check-box"></span>`
			: `<span data-bind="css: { selected: ${field}}" class="fa fa-check custom-check-box readonly"></span>`;
	};

	return {
		renderCheckbox,
		renderConceptSetCheckbox
	};
});
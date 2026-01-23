define([], function () {

	const basicLayout = {
		top2Start: ['buttons', 'pageLength'],
		top2End: 'search',
		topStart: 'info',
		topEnd: 'paging',
		bottomStart: null,
		bottomEnd: null,
	};

	const basicLayoutWithoutButtons = {
		top2Start: 'pageLength',
		top2End: 'search',
		topStart: 'info',
		topEnd: 'paging',
		bottomStart: null,
		bottomEnd: null,
	};

	const basicLayoutWithPagingAtBottom = {
		topStart: ['buttons', 'pageLength'],
		topEnd: 'search',
		bottomStart: 'info',
		bottomEnd: 'paging',
	};

	const facetedTableLayout = {
		top2Start: ['buttons', 'pageLength'],
		top2End: 'search',
		topStart: 'info',
		topEnd: 'paging',
		bottomStart: 'info',
		bottomEnd: 'paging',
	};

	const tableOnly = {
		topStart: null,
		topEnd: null,
		bottomStart: null,
		bottomEnd: null,
	};

	return {
		basicLayout,
		basicLayoutWithoutButtons,
		basicLayoutWithPagingAtBottom,
		facetedTableLayout,
		tableOnly,
	};
});

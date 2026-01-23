define([
	'knockout',
	'text!./job-manager.html',
	'pages/Page',
	'utils/AutoBind',
	'utils/CommonUtils',
	'services/Jobs',
	'appConfig',
	'services/MomentAPI',
	'services/AuthAPI',
	'services/DatatableLayoutService',
	'databindings',
	'components/ac-access-denied',
	'components/heading',
],
	function (
		ko,
		view,
		Page,
		AutoBind,
		commonUtils,
		jobsService,
		config,
		momentApi,
		authApi,
		layoutService,
	) {
	class JobManager extends AutoBind(Page) {
		constructor(params) {
			super(params);
			this.jobs = ko.observableArray([]);
			this.tableOptions = commonUtils.getTableOptions('L');
			this.columns = ko.observableArray([
				{title: ko.i18n('columns.executionId', 'Execution Id'), data: 'executionId'},
				{title: ko.i18n('columns.jobName', 'Job Name'), data: 'jobParameters.jobName.value'},
				{title: ko.i18n('columns.status', 'Status'), data: 'status'},
				{title: ko.i18n('columns.startDate', 'Start Date'), data: 'startDate'},
				{title: ko.i18n('columns.endDate', 'End Date'), data: 'endDate'}
			]);
			if (config.userAuthenticationEnabled) {
				// Add 'Author' column after 'Status' column
				this.columns.splice(3, 0, {title: ko.i18n('columns.author', 'Author'), data: 'jobParameters.jobAuthor.value', 'defaultContent': ''});
			}
			this.isAuthenticated = authApi.isAuthenticated;
			this.canReadJobs = ko.observable(true);
			this.layout = layoutService.basicLayout;
			this.updateJobs();
		}

		async updateJobs() {
			try {
				const jobs = await jobsService.getList();
				this.jobs(jobs.map((job) => {
					const { startDate = null, endDate = null } = job;
					job.startDate = startDate ? momentApi.formatDateTime(new Date(startDate)) : '-';
					job.endDate = endDate && (endDate > startDate) ? momentApi.formatDateTime(new Date(endDate)) : '-';
					job.jobParameters.jobName == undefined && (job.jobParameters.jobName = 'n/a');
					return job;
				}));
			} catch (e) {
				if (e.status === 403) {
					this.canReadJobs(false);
				}
			}
		}

	}

	return commonUtils.build('job-manager', JobManager, view);
});

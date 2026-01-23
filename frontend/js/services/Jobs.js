define(function (require, exports) {
	const httpService = require('services/http');
	const config = require('appConfig');

	return class JobsService {
		static getJobsPath = () =>
			`${config.api.url}job/allExecutions`;
		static getJobPath = (id) => `${config.api.url}job/${id}`;
		static getJobByNamePath = (name, type) =>
			`${config.api.url}job/type/${type}/name/${name}`;

		static getList() {
			return httpService.doGet(this.getJobsPath()).then((response) => response.data);
		}

		static get(id) {
			return httpService.doGet(this.getJobPath(id));
		}

		static getByName(name, type) {
			return httpService.doGet(this.getJobByNamePath(name, type));
		}
	};
});

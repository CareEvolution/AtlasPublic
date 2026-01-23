define([
	'knockout',
	'utils/AutoBind',
	'components/Component',
	'utils/CommonUtils',
	'services/Sample',
	'utils/DatatableUtils',
	'atlas-state',
	'text!./cohort-samples.html',
	'faceted-datatable',
	'components/modal',
], function (
	ko,
	AutoBind,
	Component,
	commonUtils,
	sampleService,
	datatableUtils,
	sharedState,
	view
) {
	function gender(code) {
		if (code == 8507) return 'Male';
		if (code == 8532) return 'Female';
		else return 'Other';
	}

	function mapSampleListData(originalData) {
		return originalData.map((el) => {
			let selectionCriteria;
			let mode;
			if (el.age) {
				switch (el.age.mode) {
					case 'between':
						mode = 'Between';
						break;
					case 'notBetween':
						mode = 'Not between';
						break;
					case 'lessThan':
						mode = 'Less than';
						break;
					case 'lessThanOrEqual':
						mode = 'Less than or equal';
						break;
					case 'equalTo':
						mode = 'Equal';
						break;
					case 'greaterThan':
						mode = 'Greater than';
						break;
					case 'greaterThanOrEqual':
						mode = 'Greater than or equal';
						break;
					default:
						break;
				}
			} else {
				mode = '';
			}
			if ((mode == 'Between' || mode == 'Not between') && el.age) {
				selectionCriteria = `${mode} ${el.age.min} and ${el.age.max}`;
			} else if (el.age) {
				selectionCriteria = `${mode} ${el.age.value}`;
			} else {
				selectionCriteria = 'Any age';
			}
			if (el.gender.otherNonBinary && el.gender.conceptIds.length == 2) {
				selectionCriteria = `Any Gender, ${selectionCriteria}`;
			} else {
				if (el.gender.otherNonBinary) {
					selectionCriteria = `Other, ${selectionCriteria}`;
				}
				if (el.gender.conceptIds[0]) {
					selectionCriteria = `${gender(
						el.gender.conceptIds[0]
					)}, ${selectionCriteria}`;
				}
				if (el.gender.conceptIds[1]) {
					selectionCriteria = `${gender(
						el.gender.conceptIds[1]
					)}, ${selectionCriteria}`;
				}
			}
			const sampleId = el.id;
			const sampleName = el.name || '';
			const patientCounts = el.size;
			const createdBy = (el.createdBy && el.createdBy.name) || '';
			const createdOn = new Date(el.createdDate).toLocaleString();
			return {
				sampleId,
				sampleName,
				selectionCriteria,
				patientCounts,
				createdBy,
				createdOn,
			};
		});
	}

	class CohortSamples extends AutoBind(Component) {
		constructor(params) {
			super(params);

			this.currentCohortDefinition = params.cohortDefinition;
			this.cdmSources = params.cdmSources;

			this.cohortDefinitionSourceInfo =
				sharedState.CohortDefinition.sourceInfo;

			this.sampleSourceKey = params.selectedSourceKey;
			this.isLoadingSampleData = ko.observable(false);

			//sampleSourceKey changes => get list of samples
			this.subscriptions.push(
				this.sampleSourceKey.subscribe((val) => {
					const cohortId = this.currentCohortDefinition().id();

					if (cohortId && val) {
						history.pushState(
							null,
							'',
							`#/cohortdefinition/${cohortId}/samples/${val}`
						);
						this.selectedSampleId(null);
						this.getSampleList(cohortId);
					} else if (cohortId) {
						history.pushState(
							null,
							'',
							`#/cohortdefinition/${cohortId}/samples/`
						);
					} else {
						history.pushState(null, '', `#/cohortdefinitions/`);
					}
				})
			);

			// new sample state
			this.showSampleCreatingModal = ko.observable(false);
			this.isSampleGenerating = ko.observable(false);
			this.newSampleCreatingLoader = ko.observable(false);
			this.sampleName = ko.observable('');
			this.patientCount = ko.observable();
			this.sampleAgeType = ko.observable('');
			this.firstAge = ko.observable();
			this.secondAge = ko.observable();
			this.isMaleSample = ko.observable(false);
			this.isFeMaleSample = ko.observable(false);
			this.isOtherGenderSample = ko.observable(false);
			//error state
			this.sampleNameError = ko.pureComputed(
				() => this.sampleName().trim() == ''
			);
			this.patientCountError = ko.pureComputed(
				() => !(this.patientCount() > 0)
			); // this works because null == 0
			this.isAgeRange = ko.pureComputed(() =>
				['between', 'notBetween'].includes(this.sampleAgeType())
			);
			this.firstAgeError = ko.pureComputed(
				() => this.firstAge() != null && this.firstAge() < 0
			);
			this.isAgeRangeError = ko.pureComputed(
				() =>
					this.isAgeRange() && // age range selected
					!(this.firstAge() == null && this.secondAge() == null) && // one is non-null
					(this.firstAge() == null ||
						this.secondAge() == null ||
						this.firstAge() < 0 ||
						this.secondAge() < 0 ||
						this.firstAge() == this.secondAge())
			); //  has invalid value

			//sample list
			this.sampleListCols = [
				{
					title: ko.i18n('columns.sampleId', 'Sample Id'),
					data: 'sampleId',
					visible: false,
				},
				{
					title: ko.i18n('columns.sampleName', 'Sample name'),
					render: datatableUtils.getLinkFormatter((d) => ({
						label: d['sampleName'],
						linkish: true,
					})),
				},
				{
					title: ko.i18n(
						'columns.numberOfPatients',
						'Number of patients'
					),
					data: 'patientCounts',
				},
				{
					title: ko.i18n(
						'columns.selectionCriteria',
						'Selection criteria'
					),
					data: 'selectionCriteria',
				},
				{
					title: ko.i18n('columns.author', 'Author'),
					data: 'createdBy',
				},
				{
					title: ko.i18n('columns.created', 'Created'),
					data: 'createdOn',
				},
				{
					title: ko.i18n('columns.action', 'Action'),
					sortable: false,
					render: function () {
						return `<i class="sample-list fa fa-trash" aria-hidden="true"></i> <i class="sample-list fa fa-refresh" aria-hidden="true"></i>`;
					},
				},
			];
			this.sampleList = ko.observableArray();
			this.tableOptions = commonUtils.getTableOptions('L');

			// Sample data table
			this.sampleCols = [
				{
					title: ko.i18n('columns.personId', 'Person ID'),
					render: datatableUtils.getLinkFormatter((d) => ({
						label: d['personId'],
						linkish: true,
					})),
				},
				{
					title: ko.i18n('columns.gender', 'Gender'),
					data: 'gender',
				},
				{
					title: ko.i18n('columns.ageAtIndex', 'Age at index'),
					data: 'ageIndex',
				},
			];
			this.sampleData = ko.observableArray([]);

			this.selectedSampleId = params.selectedSampleId;
			this.sampleDataLoading = ko.observable(false);

			this.subscriptions.push(
				this.selectedSampleId.subscribe((val) => {
					const cohortId = this.currentCohortDefinition().id();
					const sampleSourceKey = this.sampleSourceKey();

					if (cohortId && sampleSourceKey && val) {
						history.pushState(
							null,
							'',
							`#/cohortdefinition/${cohortId}/samples/${sampleSourceKey}/${val}`
						);
						this.fetchSampleData({
							sampleId: val,
							sourceKey: sampleSourceKey,
							cohortDefinitionId: cohortId,
						});
					} else if (cohortId && sampleSourceKey) {
						history.pushState(
							null,
							'',
							`#/cohortdefinition/${cohortId}/samples/${sampleSourceKey}`
						);
					} else if (cohortId) {
						history.pushState(
							null,
							'',
							`#/cohortdefinition/${cohortId}/samples/`
						);
					} else {
						history.pushState(null, '', `#/cohortdefinitions/`);
					}
				})
			);

			this.isCohortGenerated = ko.pureComputed(() => {
				const sourceInfo = this.cohortDefinitionSourceInfo().find(
					(d) => d.sourceKey == this.sampleSourceKey()
				);
				return sourceInfo && sourceInfo.status() === 'COMPLETE';
			});

			//end of sample states

			if (this.sampleSourceKey()) {
				this.getSampleList(this.currentCohortDefinition().id());
			}
			if (this.sampleSourceKey() && this.selectedSampleId()) {
				this.fetchSampleData({
					sampleId: this.selectedSampleId(),
					sourceKey: this.sampleSourceKey(),
					cohortDefinitionId: this.currentCohortDefinition().id(),
				});
			}
		}

		addNewSample() {
			this.resetSampleForm();
			this.showSampleCreatingModal(true);
		}

		isSampleFormValid() {
			// if a mandotory field is not yet filled at all, it should be error
			return !(
				this.sampleNameError() ||
				this.patientCountError() ||
				this.firstAgeError() ||
				this.isAgeRangeError()
			);
		}

		resetSampleForm() {
			this.sampleName('');
			this.patientCount('');
			this.sampleAgeType('lessThan');
			this.firstAge(null);
			this.secondAge(null);
			this.isMaleSample(false);
			this.isFeMaleSample(false);
			this.isOtherGenderSample(false);
		}

		async createNewSample() {
			if (!this.isSampleFormValid()) {
				// do nothing
				return;
			}

			const cohortDefinitionId = this.currentCohortDefinition().id();
			const sourceKey = this.sampleSourceKey();
			const name = this.sampleName();
			const size = Number(this.patientCount());
			const ageMode = this.sampleAgeType();
			const conceptIds = [];
			let otherNonBinary = false;
			const selectAllGender =
				!this.isMaleSample() &&
				!this.isFeMaleSample() &&
				!this.isOtherGenderSample();
			if (this.isMaleSample() || selectAllGender) {
				conceptIds.push(8507);
			}
			if (this.isFeMaleSample() || selectAllGender) {
				conceptIds.push(8532);
			}
			if (this.isOtherGenderSample() || selectAllGender) {
				otherNonBinary = true;
			}

			const firstAge = this.firstAge();
			const secondAge = this.secondAge();
			let age;
			if (
				(this.isAgeRange() && firstAge == null && secondAge == null) ||
				(!this.isAgeRange() && firstAge == null)
			) {
				age = null;
			} else {
				age = {
					value: this.isAgeRange() ? null : firstAge,
					mode: ageMode,
					min: this.isAgeRange()
						? firstAge < secondAge
							? firstAge
							: secondAge
						: null,
					max: this.isAgeRange()
						? firstAge < secondAge
							? secondAge
							: firstAge
						: null,
				};
			}

			const payload = {
				name,
				size,
				age,
				gender: { otherNonBinary, conceptIds },
			};
			this.newSampleCreatingLoader(true);
			try {
				const res = await sampleService.createSample(payload, {
					cohortDefinitionId,
					sourceKey,
				});
				const newData = mapSampleListData([res]);
				this.sampleList.unshift(...newData);
				this.showSampleCreatingModal(false);
			} catch (error) {
				console.error(error);
				alert(
					error &&
						error.data &&
						error.data.payload &&
						error.data.payload.message
						? error.data.payload.message
						: 'Error when creating sample, please try again later'
				);
			} finally {
				this.newSampleCreatingLoader(false);
			}
		}

		getSampleList(cohortId) {
			const cohortDefinitionId =
				cohortId || this.currentCohortDefinition().id();
			const sourceKey = this.sampleSourceKey();
			if (!sourceKey || !this.isCohortGenerated()) {
				this.sampleList([]);
				return;
			}
			this.isLoadingSampleData(true);
			sampleService
				.getSampleList({ cohortDefinitionId, sourceKey })
				.then((res) => {
					if (res.generationStatus != 'COMPLETE') {
						this.sampleSourceKey(null);
						alert(
							ko.i18n(
								'cohortDefinitions.cohortDefinitionManager.samples.cohortShouldBeGenerated',
								'Cohort should be generated before creating samples'
							)()
						);
						return;
					}
					const sampleListData = mapSampleListData(res.samples);
					this.sampleList(sampleListData);
				})
				.catch((error) => {
					console.error(error);
					alert(
						ko.i18n(
							'cohortDefinitions.cohortDefinitionManager.samples.errorFetchingList',
							'Error when fetching sample list, please try again later'
						)()
					);
				})
				.finally(() => {
					this.isLoadingSampleData(false);
				});
		}

		onSampleListRowClick(d, e) {
			// find index of click
			const { sampleId } = d;
			const rowIndex = this.sampleList().findIndex(
				(el) => el.sampleId == sampleId
			);

			const cohortDefinitionId = this.currentCohortDefinition().id();
			const sourceKey = this.sampleSourceKey();
			if (e.target.className == 'sample-list fa fa-trash') {
				// todo: close existing sample
				if (this.selectedSampleId() == sampleId) {
					this.sampleData([]);
					this.selectedSampleId(null);
					commonUtils.routeTo(
						`/cohortdefinition/${cohortDefinitionId}/samples/${sourceKey}/`
					);
				}
				sampleService
					.deleteSample({ cohortDefinitionId, sourceKey, sampleId })
					.then((res) => {
						if (res.ok) {
							this.sampleList.splice(rowIndex, 1);
						}
					})
					.catch(() => {
						alert(
							ko.i18n(
								'cohortDefinitions.cohortDefinitionManager.samples.errorDeleting',
								'Error when deleting sample, please try again later'
							)()
						);
					});
			} else if (e.target.className == 'sample-list fa fa-refresh') {
				this.sampleDataLoading(true);
				this.refreshSample({ sampleId, sourceKey, cohortDefinitionId })
					.then((res) => {
						this.showSampleDataTable(res.elements);
					})
					.finally(() => {
						this.sampleDataLoading(false);
					});
			} else {
				this.fetchSampleData({
					sampleId,
					sourceKey,
					cohortDefinitionId,
				});
			}
		}

		fetchSampleData({ sampleId, sourceKey, cohortDefinitionId }) {
			this.sampleDataLoading(true);
			sampleService
				.getSample({ cohortDefinitionId, sourceKey, sampleId })
				.then((res) => {
					this.selectedSampleId(sampleId);
					this.showSampleDataTable(res.elements);
					history.pushState(
						null,
						'',
						`#/cohortdefinition/${cohortDefinitionId}/samples/${sourceKey}/${sampleId}`
					);
				})
				.catch((error) => {
					console.error(error);
					alert(
						ko.i18n(
							'cohortDefinitions.cohortDefinitionManager.samples.errorFetchingData',
							'Error when fetching sample data, please try again later'
						)()
					);
				})
				.finally(() => {
					this.sampleDataLoading(false);
				});
		}

		refreshSample({ sampleId, sourceKey, cohortDefinitionId }) {
			return sampleService
				.refreshSample({ cohortDefinitionId, sourceKey, sampleId })
				.catch(() => {
					alert(
						ko.i18n(
							'cohortDefinitions.cohortDefinitionManager.samples.errorRefreshing',
							'Error when refreshing sample, please try again later'
						)()
					);
				});
		}

		onSampleDataClick(d) {
			const sampleId = this.selectedSampleId();
			const cohortDefinitionId = this.currentCohortDefinition().id();
			const sourceKey = this.sampleSourceKey();
			window.open(
				`#/profiles/${sourceKey}/${d.personId}/${cohortDefinitionId}/${sampleId}`
			);
		}

		showSampleDataTable(sample) {
			const transformedSampleData = sample.map((el) => ({
				personId: el.personId,
				gender: gender(el.genderConceptId),
				ageIndex: el.age,
			}));

			this.sampleData(transformedSampleData);
		}
	}

	return commonUtils.build('cohort-samples', CohortSamples, view);
});
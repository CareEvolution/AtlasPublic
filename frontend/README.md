# NOTE -- BEFORE TRYING TO BUILD AND RUN

Atlas depends on the AtlasVisualizations library, which is available at npmjs.com. It must be updated using the code in the - [AtlasVisualizationsPublic](https://github.com/CareEvolution/AtlasVisualizationsPublic "AtlasVisualizationsPublic") repository, and the package.json file in this repository should be updated to reference the correct version. 


# ATLAS

<a href="http://www.ohdsi.org/web/atlas"><img src="http://www.ohdsi.org/web/wiki/lib/exe/fetch.php?cache=&media=documentation:software:logo_without_text.png" align="left" hspace="10" vspace="6" width="164" height="200"></a>

**ATLAS** is an open source software tool for researchers to conduct scientific analyses on standardized observational data converted to the [OMOP Common Data Model V5](https://github.com/OHDSI/CommonDataModel/wiki "OMOP Common Data Model V5"). Researchers can create cohorts by defining groups of people based on an exposure to a drug or diagnosis of a particular condition using healthcare claims data. ATLAS has vocabulary searching of medical concepts to identify people with specific conditions, drug exposures etc. Patient profiles can be viewed within a specific cohort allowing visualization of a particular subject's health care records. Population effect level estimation analyses allows for comparison of two different cohorts and leverages R packages.

## Quick Start For Developers

### Prerequisites
Atlas requires the WebAPI backend service, which is in the `backend` directory in this repository. See the instructions there to get the service running in your development environment.

### Configuration
The default configuration for ATLAS is found in the /js/config/app.js file. To override these application settings, create a new file in /js/config-local.js (example below). Please note: the file /js/config-local.js is not included in the Atlas repository. This is by design so that you may have environment-specific configuration.

```javascript
define([], function () {
	var configLocal = {};

	configLocal.userAuthenticationEnabled = true;

	configLocal.api = {
		name: 'Local',
		url: 'http://localhost:8080/WebAPI/'
	};

	configLocal.authProviders = [{
		"name": "DB",
		"url": "user/login/db",
		"ajax": true,
		"icon": "fa fa-database",
		"isUseCredentialsForm": true
	}];

	return configLocal;
});
```

In the example above, the /js/config-local.js overrides the corresponding ".api" property found in /js/config/app.js and allows for custom configuration for your environment. The same pattern can be used to override the other settings in /js/config/app.js.

### Run

You are now ready to run Atlas through the vite development server.

```bash
npm ci
npm run genversion
npx vite
```

In your browser, navigate to

`http://localhost:5173/`

## Production Build

The `npm run build` script compiles and minifies the javascript, css, and assets and puts the output in the `\dist` folder. This folder can be copied to a web server for deployment.

We also have a Docker build process.

Github actions are included in the repository for building Atlas on commits and PRs.

## Resources

* [Atlas Demo (ohdsi.org)](http://atlas-demo.ohdsi.org/) This is the old version of Atlas, and is good for comparison with this updated version. **Please note: Google Chrome is the recommended browser for OHDSI's ATLAS implementation.**
* [Documentation](https://github.com/OHDSI/Atlas/wiki) This is the OHDSI documentation for how to use Atlas.
* Developer questions/comments/feedback: <a href="http://forums.ohdsi.org/c/developers">OHDSI Forum</a>

## Technology

ATLAS is built using HTML, CSS and [Knockout JavaScript](http://knockoutjs.com/ "Knockout JavaScript"). 

## Dependencies
- [WebAPI](https://github.com/CareEvolution/AtlasPublic/backend "WebAPI")

## License
ATLAS is licensed under Apache License 2.

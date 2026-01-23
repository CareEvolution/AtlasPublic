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
npm run build
npx vite
```

Note that we are using vite as a development web server. This will allow us to make changes to the javascript files and they will be automatically reloaded by the browser.

In your browser, navigate to

`http://localhost:5173/`

### Troubleshooting vite

If you get an error like the following, the symlinks when cloning the repo did not persist correctly. Fix below>

![Symlink error seen after trying to load Atlas from vite](https://slabstatic.com/prod/assets/5jsoecfx/post/dauh9nuf/preimages/aHDKqZw47pNZNqdKEpAYO22F.png?jwt=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOiJodHRwczovL3NsYWJzdGF0aWMuY29tIiwiZXhwIjoxNzcwNDAwMjUyLCJpYXQiOjE3NjkxOTA2NTIsImlzcyI6Imh0dHBzOi8vYXBwLnNsYWIuY29tIiwianRpIjoiMzI2bXIxamwxc2p1bG5vN29iMnZjc2Q0IiwibmJmIjoxNzY5MTkwNjUyLCJwYXRoIjoicHJvZC9hc3NldHMvNWpzb2VjZngvcG9zdC9kYXVoOW51ZiJ9.T_4zhdjCIlenKaDTgOzpbuWfX3VOzc0u6s8Z81HdV3icy8GkHLuYeOosxyhQsM2FaADeHTfFonIWLhp07bWEDdzLI8LnzBTpAuW1GBQmmXMYawwxOVGdO5FOnlSQIoyaEikBSG0dz-gp6pZzcD7sXdPs7sdndx_jua6sQ9_9EsTD6XVXJT-1eGVaULag635RgdXSlZJ4i53KyaSq2ygWb5mOvJs_XQX2lpdCUySF5IaD04DD01pUCzpgfSyOxfJB60UyVAwyV1cm7PJ_W-pn0N8j5Uex5uR-hUwl6xyiombwcE8U7zTJ2mNiriax_qoTKkivgc0OEmiAJLiOEoxS9Q)

in DFGAtlas/frontend, delete the `require.js` file and then run the following in an elevated Command Prompt (*not* Powershell)

`mklink /h require.js .\node_modules\requirejs\require.js`

Then CD to DFGAtlas\frontend\styles\vendor, delete the `font-awesome` and `jquery-ui` files and run the following:

`mklink /d font-awesome ..\..\node_modules\font-awesome`

`mklink /d jquery-ui ..\..\node_modules\jquery-ui`


## Production Build

The `npm run build` script compiles and minifies the javascript, css, and assets and puts the output in the `\dist` folder. This folder can be copied to a web server for deployment.

We also have a Docker build process.

Github actions are included in the repository for building Atlas when tagged.

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

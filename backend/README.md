# OHDSI WebAPI

OHDSI WebAPI contains all OHDSI RESTful services that can be called from OHDSI applications.

## Running this code

The WebAPI config is all in `source/main/resources/application.yaml`. You can modify this file directly, but a better approach is to use environment variables to override the settings as needed in your dev environment. Set up a configuration in your `./vscode/launch.json` file with something like this:

```
        {
            "type": "java",
            "name": "WebapiApplication",
            "request": "launch",
            "cwd": "${workspaceFolder}/backend",
            "mainClass": "org.ohdsi.webapi.WebapiApplication",
            "projectName": "webapi",
            "env": {
                "WEBAPI_APP-DB_USERNAME": <your user name if different from what's in application.yml>,
                "WEBAPI_APP-DB_PASSWORD": <your password if different from what's in application.yml>,
                "WEBAPI_HERACLES_SMALL-CELL-COUNT": "1",
                "LOGGING_FORMAT_CONSOLE": "HumanConsole",
                "LOGGING_LEVEL_ROOT": "INFO",
                "WEBAPI_SECURITY_MODE": <one of anonymous, database, or oauth>,
                "WEBAPI_SECURITY_AUTH-DB_USERNAME": <your user name, if using database security>,
                "WEBAPI_SECURITY_AUTH-DB_PASSWORD": <your password, if using database security>,
                "WEBAPI_SECURITY_AUTH-DB_SCHEMA-NAME": <schema where your users table resides>,
                "WEBAPI_SECURITY_CORS_ALLOWED-ORIGINS": "http://localhost:5173",
                "SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_MY-ORGANIZATION_CLIENT-ID": <client ID for your OAUTH2 identity server>,
                "SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_MY-ORGANIZATION_CLIENT-SECRET": <client secret for your OAUTH2 identity server>,
                "SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_MY-ORGANIZATION_SCOPE": "openid,profile,email",
                "SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_MY-ORGANIZATION_AUTHORIZATION_GRANT-TYPE": "authorization_code",
                "SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_MY-ORGANIZATION_ISSUER-URI": <url for your OAUTH2 identity server>
            }
        },

```

Then `F5` will start the webapi in Debug mode.

The first time that the backend code runs it will create all of the database tables in the webapi schema the you created above.

You can manually install and build the backend code with:

`mvn package -DskipTests=true -DskipUnitTests=true`

The integration tests require Docker. Make sure it is running if you wish to run tests.

## Features

- Provides a centralized API for working with 1 or more databases converted to the [Common Data Model](https://github.com/OHDSI/CommonDataModel) (CDM) v5.
- Searching the OMOP standardized vocabularies for medical concepts and constructing concept sets.
- Defining cohort definitions for use in identifying patient populations.
- Characterizing cohorts

## Technology

OHDSI WebAPI is a Java 21 Spring Boot web application that utilizes a PostgreSQL database for storage.

## API Documentation

The API Documentation is found at [http://webapidoc.ohdsi.org/](http://webapidoc.ohdsi.org/)

## System Requirements & Installation

See the README file in the root directory of this repository

## Geospatial support

Instructions can be found at [webapi-component-geospatial](https://github.com/OHDSI/webapi-component-geospatial)

## Testing

It was chosen to use embedded PG instead of H2 for unit tests since H2 doesn't support window functions, `md5` function, HEX to BIT conversion, `setval`, `set datestyle`, CTAS + CTE.

## Support

- Developer questions/comments/feedback: [OHDSI forum](http://forums.ohdsi.org/c/developers)
- We use the [GitHub issue tracker](https://github.com/OHDSI/WebAPI/issues) for all bugs/issues/enhancements.

## Contribution

### Versioning

- WebAPI follows [Semantic versioning](https://semver.org/);
- Only Non-SNAPSHOT dependencies should be presented in POM.xml on release branches/tags.

## License
OHDSI WebAPI is licensed under Apache License 2.0
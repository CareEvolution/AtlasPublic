# Atlas

Atlas is a WYSIWYG web app that makes it easier for researchers to create cohorts from and run analyses against their OMOP CDMs. It provides a graphical user interface to various Java packages that generate SQL queries for selecting cohorts and characterizing their healthcare data. These packages have been developed by the OHDSI organization. Atlas was originally developed as open source software by OHDSI. 

This repository has both the backend and frontend code for the Atlas software. To download this repository, use

# Prerequisites

You will need to install the following tools before working with Atlas.

## VS Code

We recommend using VS Code for your IDE. You will be able to edit and debug both the WebAPI and Atlas code with it. [Download VS Code here](https://code.visualstudio.com/download).

You can use a different IDE or text editor if you prefer, but these instructions assume that you are using VS Code.

## Git

Git is used for version control. All of the OHDSI code is available through GitHub. [Download Git here](https://gitforwindows.org/).

## Java

**Java 21** or later is required for building the WebAPI code. [Download the latest Java SDK here.](https://adoptium.net)

## Apache Maven

Maven is used to build the WebAPI executable from the WebAPI code. [Installation instructions are here.](https://maven.apache.org/install.html)

For Windows, there is no MSI installer for Maven. You can use the latest version of Maven. [Download the compiled Maven binary here](https://maven.apache.org/download.cgi).

- Download the binary zip archive and unzip it in `C:\Program Files`. 
- Add the bin directory inside the unzipped directory to your `PATH` environment variable.

## PostgreSQL and PgAdmin

Postgres hosts the WebAPI configuration database. It can also be used to host your patient data converted to the Common Data Model, but if you prefer you can store that data in another database platform such as SQL Server or Redshift. This documentation will describe how to use Postgres for patient data. You can use the latest version of Postgres. [Download PostgreSQL and PgAdmin here](https://www.postgresql.org/download/windows/).

The PostgreSQL installer includes the option to install PgAdmin, which is a database query tool specific to PostgreSQL. We recommend that you install it and use it when you need to manually run SQL scripts or inspect your WebAPI or patient data DBs. But you are free to use another database query tool if you prefer.

## VS Code Extensions

You will most likely want to run the code through a debugger. The instructions that follow will explain how to do this using VS Code, but it requires that you first install some extensions.

- [Extension Pack for Java](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack) - This provides tools for working with Java code.
- [Spring Boot Extensions ](https://code.visualstudio.com/docs/java/java-spring-boot)- This allows you to work effectively with Spring Boot apps.

## Docker

Integration tests require Docker to be installed and running. [Get Docker here.](https://www.docker.com/products/docker-desktop/)


## Set up the DB

You need to set up a database and schema for Atlas in PostgreSQL. Use a tool such as PgAdmin or DBeaver to connect to your local Postgres deployment.

### Create Roles and Logins

As a superuser, execute the following SQL to create 2 roles and 2 logins for the database server (feel free to change the passwords to something more secure):

```
CREATE ROLE ohdsi_admin 
WITH 
  NOLOGIN 
  NOSUPERUSER 
  CREATEDB 
  NOCREATEROLE 
  INHERIT 
  NOREPLICATION 
  NOBYPASSRLS 
CONNECTION LIMIT -1; 

COMMENT ON ROLE ohdsi_admin IS 'Administration group for OHDSI applications';

CREATE ROLE ohdsi_app 
WITH 
  NOLOGIN 
  NOSUPERUSER 
  NOCREATEDB 
  NOCREATEROLE 
  INHERIT 
  NOREPLICATION 
  NOBYPASSRLS 
CONNECTION LIMIT -1; 

COMMENT ON ROLE ohdsi_app IS 'Application group for OHDSI applications';

CREATE ROLE ohdsi_admin_user 
WITH 
  LOGIN 
  NOSUPERUSER 
  CREATEDB 
  NOCREATEROLE 
  INHERIT 
  NOREPLICATION 
  NOBYPASSRLS 
CONNECTION LIMIT -1 PASSWORD 'admin1';

GRANT ohdsi_admin TO ohdsi_admin_user; 
COMMENT ON ROLE ohdsi_admin_user IS 'Admin user account for OHDSI applications';

CREATE ROLE ohdsi_app_user 
WITH 
  LOGIN 
  NOSUPERUSER 
  NOCREATEDB 
  NOCREATEROLE 
  INHERIT 
  NOREPLICATION 
  NOBYPASSRLS 
CONNECTION LIMIT -1 PASSWORD 'app1';

GRANT ohdsi_app TO ohdsi_app_user; 
COMMENT ON ROLE ohdsi_app_user IS 'Application user account for OHDSI applications';
```

### Create database and schema

Now connect to your local Postgres deployment as the ohdsi_admin_user that you created above . **Make sure you are connected using the `ohdsi_admin_user` for the remainder of these instructions.**

Execute the following SQL to create a database for Atlas (you can change the database name if you like):

```
CREATE DATABASE ohdsi 
WITH OWNER = ohdsi_admin_user 
ENCODING = 'UTF8' 
LOCALE_PROVIDER = 'libc' 
CONNECTION LIMIT = -1 
IS_TEMPLATE = False;

COMMENT ON DATABASE ohdsi IS 'OHDSI WebAPI database';
GRANT ALL ON DATABASE ohdsi TO ohdsi_admin;
GRANT TEMPORARY, CONNECT ON DATABASE ohdsi TO ohdsi_app;
```

Create the webapi schema by executing the following SQL from within your new database:

```
CREATE SCHEMA webapi AUTHORIZATION ohdsi_admin;
COMMENT ON SCHEMA webapi IS 'Schema containing tables to support WebAPI functionality';
GRANT USAGE ON SCHEMA webapi TO PUBLIC;
GRANT ALL ON SCHEMA webapi TO ohdsi_admin;
GRANT USAGE ON SCHEMA webapi TO ohdsi_app;
ALTER DEFAULT PRIVILEGES FOR ROLE ohdsi_admin_user IN SCHEMA webapi GRANT DELETE, INSERT, REFERENCES, SELECT, TRIGGER, UPDATE ON TABLES TO ohdsi_app;
ALTER DEFAULT PRIVILEGES FOR ROLE ohdsi_admin_user IN SCHEMA webapi GRANT SELECT, USAGE ON SEQUENCES TO ohdsi_app;
ALTER DEFAULT PRIVILEGES FOR ROLE ohdsi_admin_user IN SCHEMA webapi GRANT EXECUTE ON FUNCTIONS TO ohdsi_app;
ALTER DEFAULT PRIVILEGES FOR ROLE ohdsi_admin_user IN SCHEMA webapi GRANT USAGE ON TYPES TO ohdsi_app;
```

## Clone the code

Create a directory where your OHDSI git repositories will reside, eg:

```
~\Documents\GitHub\OHDSI
```

Clone the AtlasPublic repo into a folder inside of your OHDSI directory (**must run as admin** to get the core.symlinks to be properly created in Windows - this will prevent errors with later):

`git clone -c core.symlinks=true https://github.com/CareEvolution/AtlasPublic/`

This repo is organized as follows:

- backend: the backend WebAPI Java code
- frontend: the frontend Atlas JavaScript code

See the README files in the backend and frontend directories for instructions on how to run the code in each of them.

# Authentication

By default, Atlas is configured to not require authentication. However, if you enable auth you will have a more complete set of features to work with. Particularly of interest, setting up security enables the Configuration screen, which lets you manage users and configure Data Sources.

Atlas supports database and OAuth authentication.

To select your authentication mode, in your launch.json file, set the environment variable `WEBAPI_SECURITY_MODE` to one of `anonymous`, `database`, or `oauth`.

- anonymous - no user login required, but there is no access to the user/roles/permissions configuration screen
- database - user credentials are stored in a database table that you manage. This authentication implementation is not fully secure and should not be used in a production environment.
- oauth - authentication is handled by an external oauth identity server

# Set up database authentication

## Configure WebAPI

First, in your launch.json configuration, set `"WEBAPI_SECURITY_MODE": "database"`.

If you are using database authentication in your development environment you will store user credentials in a local database.

You will need to create the table to store user credentials.

- Using PgAdmin, open your OHDSI database.
- Run the following script to create a `users` table in the `webapi` schema:

```
CREATE TABLE webapi.users
(
    username character varying(255) COLLATE pg_catalog."default",
    password character varying(255) COLLATE pg_catalog."default",
    firstname character varying(255) COLLATE pg_catalog."default",
    middlename character varying(255) COLLATE pg_catalog."default",
    lastname character varying(255) COLLATE pg_catalog."default"
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

ALTER TABLE webapi.users
OWNER to ohdsi_app_user;

GRANT ALL ON TABLE webapi.users TO ohdsi_app_user WITH GRANT OPTION;
```

Now, let's add a user with username `ohdsi` and password `ohdsi` to our `users` table:

```
insert into webapi.users (username,password) 
values ('ohdsi', '$2a$04$Fg8TEiD2u/xnDzaUQFyiP.uoDu4Do/tsYkTUCWNV0zTCW3HgnbJjO')
```

The WebAPI code uses BCrypt to encrypt passwords. If you want to insert other users into your database, use [https://www.bcryptcalculator.com/](https://www.bcryptcalculator.com/) to encrypt the passwords.

Finally, you need to tell WebAPI about your users table. Add these environment variables to your launch.json configuration:

```
"WEBAPI_SECURITY_AUTH-DB_USERNAME": <username to connect to your OHDSI database>,
"WEBAPI_SECURITY_AUTH-DB_PASSWORD": <password to connect to your OHDSI database>,
"WEBAPI_SECURITY_AUTH-DB_SCHEMA-NAME": "webapi",
```

## Configure Atlas

We need to let Atlas know that authentication has been enabled. Create a `config-local.js` file in the `frontend/js` directory in the Atlas repo. Add this to the file:

```
define([], function () {
	var configLocal = {};

	configLocal.userAuthenticationEnabled = true;

	configLocal.api = {
		name: 'Local',
		url: 'http://localhost:8080/WebAPI/'
	};

	configLocal.authProviders = [
      {
	    name: 'DB',
		url: 'user/login/db',
		ajax: true,
		icon: 'fa fa-database,
		isUseCredentialsForm: true,
        withCredentials: true,
	  }
    ];

	return configLocal;
});
```

At this point you should be able to log into Atlas using your `ohdsi` user. However, you won't be able to access most features.

## Add permissions to user

If you look in the WebAPI schema of the OHDSI database, you will see that there are several tables whose names are prefixed with `sec_`. These are the permissions tables. Users are assigned roles, and roles have permissions associated with them. In order to give our ohdsi user permission to work with the various Atlas features, we must do the following:

- Check to see if there is an entry in the `sec_user` table for your `ohdsi` user. If it's not there, add the entry manually with:

```
INSERT INTO webapi.sec_user (login, name)
VALUES ('ohdsi', 'ohdsi')
```

- Add the `admin` and `Atlas users` roles to the user created above, and if you have a CDM set up, also add the role for that source (should be named something like `Source user (abc)`) 

```
INSERT INTO webapi.sec_user_role (user_id, role_id)
VALUES (1000, 2), (1000, 10)

INSERT INTO webapi.sec_user_role (user_id, role_id)
VALUES (1000, 1001) -- double check role_id against your local table
```

Now your `ohdsi` user should have the permissions necessary do things in Atlas. Try signing in.

If you see a message saying "Application initialization failed - no sources are configured" and you DO have a source in your webapi tables, it's likely that your user does not have the `Source user (abc)` permission.

# Set up oauth authentication

If you have access to an oauth identity server you can use oauth authentication. This is more secure and can be used in a production environment. In the instructions below we will create an oauth configuration named MY-ORGANIZATION.

## Configure WebAPI

First, in your launch.json configuration, set `"WEBAPI_SECURITY_MODE": "oauth"`.

You will need to tell WebAPI some basic information about the identity server that should be used for authentication. Add the following environment variables to your launch.json configuration:

```
"SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_MY-ORGANIZATON_CLIENT-ID": "<your client ID>",
"SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_MY-ORGANIZATON_CLIENT-SECRET": <your client secret>,
"SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_MY-ORGANIZATON_SCOPE": "openid,profile,email",
"SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_MY-ORGANIZATON_AUTHORIZATION_GRANT-TYPE": "authorization_code",
"SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_MY-ORGANIZATON_ISSUER-URI": "<your identity server's URL>"
```

In the environment variables, the `MY-ORGANIZATION` part of it is a unique string that identifies the identity server registration. You can configure more than one identity server if you like, but each one needs to have a unique identifier string.

The client secret needs to be unencrypted.

## Configure Atlas

We need to let Atlas know that authentication has been enabled. Create a `config-local.js` file in the `frontend/js` directory in the Atlas repo. Add this to the file:

```
define([], function () {
	var configLocal = {};

	configLocal.userAuthenticationEnabled = true;

	configLocal.api = {
		name: 'Local',
		url: 'http://localhost:8080/WebAPI/'
	};

	configLocal.authProviders = [
		{
			name: 'My Organization',
			url: 'oauth2/authorization/my-organization',
			ajax: false,
			icon: 'fa fa-openid',
			withCredentials: true,
		},
    ];

	return configLocal;
});
```

At this point you should be able to log into Atlas. You will need an account on your identity server. The first time you log in, an entry will be made for your user in the webapi.sec_user table. A set of default roles (unless otherwise configured, will only be `public` which does not include Permissions editing access) will be assigned to your user.

# http4k-sn-template

This is a template application built on the [http4k-sn stack](https://github.com/springernature/http4k-sn). It's intended to be a skeleton that you can copy and use as a starter for a new application.

It's available as a template repository in our GitHub organisation.

## What this gets you

You'll get an application using Http4k-SN, using Gradle and JDK17, with basic test dependencies, and some example code. The build pipeline uses GitHub Actions, I strongly suggest reading the overview [here](https://ee.public.springernature.app/rel-eng/github-actions/overview/) 

## How to use

* Click the `use this template` button above
* Choose a repository name
* The repository name will be used as project name in the newly created repo/project
* Wait until the GitHub Action to prepare your new project is executed
* Clone the repository
* Modify
    * `.halfpipe.io` - team name, Slack channel, Grafana URI/API key, un-comment the deploy/dashboard tasks, re-generate the GitHub Actions workflow (ie run the `halfpipe` cmd in your terminal from the root directory of your local repo)
    * `etc/manifest-live.yml` - CF parameters, service bindings, JVM parameters...
    * `etc/dependency-checks/.halfpipe.io.yml` - team name, Slack channel for deployment, Slack channel for reports, re-generate the GitHub Actions workflow
    * `etc/owasp-security-checks/.halfpipe.io.yml` - team name, Slack channel for deployment, Slack channel for reports, re-generate the GitHub Actions workflow
    * `grafana-dashboard/src/main/kotlin/com/springernature/http4k/template/Main.kt` - datasource, CF details
* Add your team as admin to the new repository in the GitHub repository settings
* Also in the repo's settings go to `https://github.com/springernature/<REPO>/settings/secrets/actions` and add the following secrets:
    ```
    VAULT_ROLE_ID
    VAULT_SECRET_ID
    ```
    You can get the value for these from here: `vault kv get springernature/<TEAM-NAME>/team-ro-app-role`
* Update this README to reflect your new project

## Prerequisites

* JDK 17 (e.g. [Liberica](https://bell-sw.com/pages/package-managers/), which matches Cloudfoundry) installed and on the `PATH`

## Artifactory

Both Gradle and IntelliJ will need the environment variables `ARTIFACTORY_PASSWORD` and `ARTIFACTORY_USERNAME` with the credentials for [Artifactory](https://springernature.jfrog.io/ui/packages) in order to build this project. This will allow them to download dependencies.

### Windows

To get the credentials and set them as environment variables:

* Make sure that Vault CLI is on the `PATH` and you are logged into Vault in the command line
* Log into VPN
* Execute the script `bin/setup-artifactory-env.bat`, this will permanently set the required system environment values.
  * Note: artifactory credentials are rotated regularly, rerun the script if artifactory authentication is failing

Please restart your IDE afterwards to ensure it is picking up the new environment variables.

## Running it locally

Run `com.springernature.http4k.template.MainKt` and go to http://localhost:8080/

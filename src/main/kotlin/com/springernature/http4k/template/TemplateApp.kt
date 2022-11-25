package com.springernature.http4k.template

import com.springernature.http4k.template.external.ContentHubApi
import com.springernature.http4k.template.external.GitHubApi
import com.springernature.http4k.template.handlers.Home
import com.springernature.http4k.template.handlers.Repositories
import com.springernature.http4k.template.handlers.Subjects
import com.springernature.http4k.service.Service
import com.springernature.http4k.service.ServiceName
import org.http4k.cloudnative.env.Environment
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.format.Jackson
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object TemplateApp {

    private val LOG: Logger = LoggerFactory.getLogger(TemplateApp::class.java)

    val APPLICATION = Service(
        ServiceName("http4k-sn-template"),
        dependencies = listOf(Services.CONTENTHUB_API.serviceName)
    )

    operator fun invoke(
        contentHubApiHttpClient: HttpHandler,
        gitHubApiHttpClient: HttpHandler,
        environment: Environment
    ): RoutingHttpHandler {
        LOG.info("Starting application with ${environment[Settings.AMOUNT_OF_CAKES]} cakes")

        val contentHubApi = ContentHubApi(contentHubApiHttpClient, Jackson)
        val gitHubApi = GitHubApi(gitHubApiHttpClient, Jackson)

        val home = Home()
        val subjects = Subjects(contentHubApi)
        val repositories = Repositories(gitHubApi)

        return routes(
            "/" bind home::root,
            "/subjects/labels" bind Method.GET to subjects::preferredLabels,
            "/github/repos/{user}" bind Method.GET to repositories::forUser
        )
    }
}

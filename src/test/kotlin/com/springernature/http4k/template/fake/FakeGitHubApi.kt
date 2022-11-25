package com.springernature.http4k.template.fake

import com.springernature.http4k.template.Services.GITHUB_API
import org.http4k.core.*
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.format.Jackson.auto
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.lens.Path
import org.http4k.routing.bind
import org.http4k.routing.routes

class FakeGitHubApi : FakeSystem(GITHUB_API) {

    private val reposForUser = mutableMapOf<String, List<Repository>>()

    override val handler: HttpHandler = routes(
        "/users/{user}/repos" bind Method.GET to { request ->
            val username = USER_PATH[request]
            val repositories = reposForUser[username] ?: listOf()
            Response(Status.OK)
                .with(CONTENT_TYPE of APPLICATION_JSON)
                .with(REPOS_RESPONSE_BODY of repositories)
        },
    )

    fun containsRepositories(user: String, repositories: List<String>) {
        reposForUser[user] = repositories.map { Repository(it) }
    }

    data class Repository(val name: String?)

    companion object {
        private val USER_PATH = Path.of("user")
        private val REPOS_RESPONSE_BODY = Body.auto<List<Repository>>().toLens()
    }

}
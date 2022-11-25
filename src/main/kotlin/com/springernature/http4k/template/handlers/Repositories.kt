package com.springernature.http4k.template.handlers

import com.springernature.http4k.template.external.GitHubApi
import org.http4k.core.*
import org.http4k.format.Jackson.auto
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.lens.Path

class Repositories(private val gitHubApi: GitHubApi) {

    fun forUser(request: Request) = USERNAME_QUERY[request].let { username ->
        gitHubApi.reposForUser(username).let { repositories ->
            Response(Status.OK)
                .with(CONTENT_TYPE of ContentType.APPLICATION_JSON)
                .with(REPOSITORIES_BODY of repositories)
        }
    }

    companion object {
        private val USERNAME_QUERY = Path.of("user")
        private val REPOSITORIES_BODY = Body.auto<List<String>>().toLens()
    }

}

package com.springernature.http4k.template.handlers

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.springernature.http4k.template.TestEnvironment
import org.http4k.core.*
import org.http4k.format.Jackson.auto
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasContentType
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RepositoriesTest {

    private val env = TestEnvironment()

    @BeforeEach
    fun `prepare data`() {
        env.gitHubApi.containsRepositories("aUser", listOf("repo-1", "repo-2"))
        env.gitHubApi.containsRepositories("a U\\/ser", listOf("repo-1", "repo-2"))
    }

    @Test
    fun `we can read the repositories for a user`() {
        val response = env.app(Request(Method.GET, "/github/repos/aUser"))

        assertThat(
            response, hasStatus(Status.OK)
                    and hasContentType(ContentType.APPLICATION_JSON)
                    and hasBody(LIST_BODY, equalTo(listOf("repo-1", "repo-2")))
        )
    }

    @Test
    fun `we get not found if the username is omitted`() {
        val response = env.app(Request(Method.GET, "/github/repos/"))

        assertThat(
            response, hasStatus(Status.NOT_FOUND))
    }

    companion object {
        private val LIST_BODY = Body.auto<List<String>>().toLens()
    }

}
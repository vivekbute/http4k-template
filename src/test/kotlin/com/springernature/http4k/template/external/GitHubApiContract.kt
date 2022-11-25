package com.springernature.http4k.template.external

import com.natpryce.hamkrest.*
import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.HttpHandler
import org.http4k.format.Jackson
import org.junit.jupiter.api.Test

abstract class GitHubApiContract {

    abstract val client: HttpHandler

    @Test
    fun `it returns the repositories for a user`() {
        val underTest = GitHubApi(client, Jackson)

        val repositories = underTest.reposForUser("octocat")

        assertThat(
            repositories, hasSize(greaterThanOrEqualTo(1))
                    and hasElement("test-repo1")
        )
    }

    @Test
    fun `it returns an empty list for an invalid username`() {
        val underTest = GitHubApi(client, Jackson)

        val repositories = underTest.reposForUser("nothingvalidhere")

        assertThat(repositories, isEmpty)
    }

}
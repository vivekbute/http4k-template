package com.springernature.http4k.template.external

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.greaterThanOrEqualTo
import com.natpryce.hamkrest.hasElement
import com.natpryce.hamkrest.hasSize
import com.springernature.http4k.template.fake.FakeGitHubApi
import org.http4k.format.Jackson
import org.junit.jupiter.api.Test

class GitHubApiFakeTest : GitHubApiContract() {

    override val client = FakeGitHubApi()

    init {
        client.containsRepositories("octocat", listOf("bosenberry-repo-1", "test-repo1"))
        client.containsRepositories("url/encoding+needed", listOf("encoded-repo-1"))
    }

    @Test
    fun `username is http encoded`() {
        val underTest = GitHubApi(client, Jackson)
        val repositories = underTest.reposForUser("url/encoding needed")

        assertThat(
            repositories, hasSize(greaterThanOrEqualTo(1))
                    and hasElement("encoded-repo-1")
        )
    }
}
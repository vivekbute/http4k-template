package com.springernature.http4k.template.handlers

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.springernature.http4k.template.TestEnvironment
import com.springernature.http4k.template.fake.FakeContentHubApi.HubSubject
import org.http4k.core.*
import org.http4k.format.Jackson.auto
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasContentType
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SubjectsTest {

    private val env = TestEnvironment()

    @BeforeEach
    fun `prepare data`() {
        env.contentHubApi.containsSubjects(
            HubSubject("cake", "Cake"),
            HubSubject("jellybeans", "Jellybeans")
        )
    }

    @Test
    fun `we can read the preferred labels for a keyword`() {
        val response = env.app(Request(Method.GET, "/subjects/labels").query("keyword", "cake"))

        assertThat(
            response, hasStatus(Status.OK)
                    and hasContentType(ContentType.APPLICATION_JSON)
                    and hasBody(LIST_BODY, equalTo(listOf("Cake")))
        )
    }

    @Test
    fun `we can read the preferred labels without a keyword`() {
        val response = env.app(Request(Method.GET, "/subjects/labels"))

        assertThat(
            response, hasStatus(Status.OK)
                    and hasContentType(ContentType.APPLICATION_JSON)
                    and hasBody(LIST_BODY, equalTo(listOf("Cake", "Jellybeans")))
        )
    }

    companion object {
        private val LIST_BODY = Body.auto<List<String>>().toLens()
    }

}
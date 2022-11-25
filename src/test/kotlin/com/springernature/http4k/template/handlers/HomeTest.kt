package com.springernature.http4k.template.handlers

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.springernature.http4k.template.TestEnvironment
import org.http4k.core.ContentType
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasContentType
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test

class HomeTest {

    private val env = TestEnvironment()

    @Test
    fun `the root page should greet the world`() {
        val response = env.app(Request(Method.GET, "/"))

        assertThat(
            response, hasStatus(Status.OK)
                    and hasContentType(ContentType.TEXT_PLAIN)
                    and hasBody("Hello world!")
        )
    }

}
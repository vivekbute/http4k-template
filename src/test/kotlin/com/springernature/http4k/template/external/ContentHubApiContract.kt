package com.springernature.http4k.template.external

import com.natpryce.hamkrest.*
import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.HttpHandler
import org.http4k.format.Jackson
import org.junit.jupiter.api.Test

abstract class ContentHubApiContract {

    abstract val client: HttpHandler

    @Test
    fun `it returns the preferred labels for a subject by keyword`() {
        val underTest = ContentHubApi(client, Jackson)

        val subjectPreferredLabels = underTest.subjectPreferredLabels("three-dimensional-imaging")

        assertThat(
            subjectPreferredLabels, hasSize(greaterThanOrEqualTo(1))
                    and hasElement("Three-dimensional imaging")
        )
    }

    @Test
    fun `it returns the preferred labels for a subject without a keyword`() {
        val underTest = ContentHubApi(client, Jackson)

        val subjectPreferredLabels = underTest.subjectPreferredLabels(null)

        assertThat(
            subjectPreferredLabels, hasSize(greaterThanOrEqualTo(1))
        )
    }

    @Test
    fun `it returns an empty list for an unmatched keyword`() {
        val underTest = ContentHubApi(client, Jackson)

        val subjectPreferredLabels = underTest.subjectPreferredLabels("nothingvalidhere")

        assertThat(subjectPreferredLabels, isEmpty)
    }

}
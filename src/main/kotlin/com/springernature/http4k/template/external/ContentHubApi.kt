package com.springernature.http4k.template.external

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.format.Jackson

class ContentHubApi(
    private val client: HttpHandler,
    private val jsonParser: Jackson
) {

    fun subjectPreferredLabels(keyword: String?) = client(
        Request(Method.GET, "/api/v1/subjects")
            .query("keywords", keyword)
    )
        .let { response ->
            if (response.status.successful) {
                val subjectsResponse = jsonParser.parse(response.bodyString())
                subjectsResponse["subjects"].mapNotNull { it["prefLabel"]?.textValue() }
            } else {
                listOf()
            }
        }

}
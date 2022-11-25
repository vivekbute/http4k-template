package com.springernature.http4k.template.external

import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.format.Jackson
import java.net.URLEncoder.encode
import java.nio.charset.StandardCharsets.UTF_8

class GitHubApi(
    private val client: HttpHandler,
    private val jsonParser: Jackson
) {

    fun reposForUser(username: String): List<String> =
        client(
            Request(
                GET,
                Uri.of("/users/${encode(username, UTF_8)}/repos")
            )
        ).let { response ->
            if (response.status.successful) {
                val subjectsResponse = jsonParser.parse(response.bodyString())
                subjectsResponse.mapNotNull { it["name"]?.textValue() }
            } else {
                listOf()
            }
        }

}
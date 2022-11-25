package com.springernature.http4k.template.handlers

import com.springernature.http4k.template.external.ContentHubApi
import org.http4k.core.*
import org.http4k.format.Jackson.auto
import org.http4k.lens.Header
import org.http4k.lens.Query

class Subjects(
    private val contentHubApi: ContentHubApi
) {

    fun preferredLabels(request: Request) = KEYWORD_QUERY[request].let { keyword ->
        contentHubApi.subjectPreferredLabels(keyword).let { labels ->
            if (labels.isEmpty()) {
                Response(Status.NOT_FOUND)
            } else {
                Response(Status.OK)
                    .with(Header.CONTENT_TYPE of ContentType.APPLICATION_JSON)
                    .with(SUBJECTS_BODY of labels)
            }
        }
    }

    companion object {
        private val KEYWORD_QUERY = Query.optional("keyword")
        private val SUBJECTS_BODY = Body.auto<List<String>>().toLens()
    }

}
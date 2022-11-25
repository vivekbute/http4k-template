package com.springernature.http4k.template.fake

import com.springernature.http4k.template.Services
import org.http4k.core.*
import org.http4k.core.Status.Companion.OK
import org.http4k.format.Jackson.auto
import org.http4k.lens.Query
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes

class FakeContentHubApi : FakeSystem(Services.CONTENTHUB_API) {
    private val subjects = mutableListOf<HubSubject>()

    override val handler: HttpHandler = routes(
        "/api/v1/subjects" bind Method.GET to { request ->
            val filteredSubjects = subjects.filter { subject ->
                KEYWORDS_QUERY(request)?.let { subject.id?.contains(it) ?: false } ?: true
            }
            Response(OK).with(
                SUBJECTS_RESPONSE_BODY of SubjectsResponse(subjects = filteredSubjects)
            )
        },
    )

    fun containsSubjects(vararg subjects: HubSubject) {
        this.subjects.addAll(subjects.asList())
    }


    data class HubSubject(val id: String?, val prefLabel: String?)

    data class SubjectsResponse(val subjects: List<HubSubject>)

    companion object {
        private val KEYWORDS_QUERY = Query.string().optional("keywords")
        private val SUBJECTS_RESPONSE_BODY = Body.auto<SubjectsResponse>().toLens()
    }
}
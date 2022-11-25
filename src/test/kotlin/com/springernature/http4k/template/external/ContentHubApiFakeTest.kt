package com.springernature.http4k.template.external

import com.springernature.http4k.template.fake.FakeContentHubApi

class ContentHubApiFakeTest : ContentHubApiContract() {

    override val client = FakeContentHubApi()

    init {
        client.containsSubjects(
            FakeContentHubApi.HubSubject("three-dimensional-imaging", "Three-dimensional imaging")
        )
    }

}

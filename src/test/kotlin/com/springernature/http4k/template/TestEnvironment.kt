package com.springernature.http4k.template

import com.springernature.http4k.template.fake.FakeContentHubApi
import com.springernature.http4k.template.fake.FakeGitHubApi
import org.http4k.core.then
import org.http4k.filter.ServerFilters

class TestEnvironment {

    private val settings = Settings.defaults

    val contentHubApi = FakeContentHubApi()
    val gitHubApi = FakeGitHubApi()

    val app = ServerFilters.CatchLensFailure
        .then(TemplateApp(contentHubApi, gitHubApi, settings))

}

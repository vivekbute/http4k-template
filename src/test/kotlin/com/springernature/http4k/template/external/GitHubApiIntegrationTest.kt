package com.springernature.http4k.template.external

import com.springernature.http4k.template.Services

class GitHubApiIntegrationTest : GitHubApiContract() {

    override val client = RealService.client(Services.GITHUB_API)

}

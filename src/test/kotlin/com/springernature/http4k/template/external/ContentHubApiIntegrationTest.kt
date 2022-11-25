package com.springernature.http4k.template.external

import com.springernature.http4k.template.Services
import org.http4k.core.HttpHandler

class ContentHubApiIntegrationTest : ContentHubApiContract() {

    override val client: HttpHandler = RealService.client(Services.CONTENTHUB_API)

}
package com.springernature.http4k.template

import com.springernature.http4k.service.NotChecked
import com.springernature.http4k.service.Service
import com.springernature.http4k.service.ServiceName
import org.http4k.core.Uri

object Services {

    val CONTENTHUB_API = Service(ServiceName("contenthub-api"))

    val GITHUB_API = Service(
        ServiceName("github-api"),
        defaultUri = Uri.of("https://api.github.com"),
        dependencyChecker = NotChecked
    )

}

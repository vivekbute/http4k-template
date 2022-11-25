package com.springernature.http4k.template

import com.springernature.http4k.Stack
import com.springernature.http4k.config.StackSettings
import com.springernature.http4k.template.TemplateApp.APPLICATION
import com.springernature.http4k.http.JettyServer
import org.http4k.cloudnative.env.Environment
import org.http4k.core.HttpHandler
import org.http4k.server.asServer

object TemplateAppStack {
    operator fun invoke(environment: Environment): HttpHandler {
        return Stack(APPLICATION, environment).let {
            it.server(
                TemplateApp(
                    it.httpClient(Services.CONTENTHUB_API),
                    it.httpClient(Services.GITHUB_API),
                    environment
                )
            )
        }
    }
}

fun main() {
    val environment = Environment.ENV.overrides(Settings.defaults)
    TemplateAppStack(environment)
        .asServer(JettyServer(port = environment[StackSettings.PORT].value))
        .start()
        .block()
}

package com.springernature.http4k.template.external

import com.springernature.http4k.config.appVersion
import com.springernature.http4k.template.Settings
import com.springernature.http4k.http.ApacheClient
import com.springernature.http4k.service.Service
import com.springernature.http4k.service.ServiceUriResolverFilter
import org.http4k.cloudnative.env.Environment
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.then
import java.time.Duration

object RealService {

    private val settings = Environment.Companion.ENV.overrides(Settings.defaults)

    private val apacheConnectorManager = ApacheClient.poolingConnectionManager(50, 50, Duration.ofSeconds(2))

    fun client(service: Service): HttpHandler = ServiceUriResolverFilter(settings, service)
        .then(Filter { next ->
            { request ->
                next(request.header("user-agent", "http4k-sn-template/test"))
            }
        })
        .then(
            ApacheClient(
                apacheConnectorManager,
                Duration.ofSeconds(5),
                Duration.ofMillis(500),
                settings.appVersion()
            )
        )

}
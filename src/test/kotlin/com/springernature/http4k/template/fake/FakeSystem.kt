package com.springernature.http4k.template.fake

import com.springernature.http4k.service.Service
import org.http4k.core.*

abstract class FakeSystem(val service: Service) : HttpHandler {

    private var status = Status.OK

    private val blowUpFilter = Filter { next ->
        {
            when (status) {
                Status.OK -> next(it)
                else -> Response(status)
            }
        }
    }

    fun blowsUpWith(new: Status) {
        status = new
    }

    abstract val handler: HttpHandler

    override fun invoke(request: Request): Response = blowUpFilter.then(handler)(request)
}
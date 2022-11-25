package com.springernature.http4k.template.handlers

import org.http4k.core.*
import org.http4k.lens.Header

class Home {

    @Suppress("UNUSED_PARAMETER")
    fun root(request: Request): Response =
        Response(Status.OK)
            .with(Header.CONTENT_TYPE of ContentType.TEXT_PLAIN)
            .body("Hello world!")

}

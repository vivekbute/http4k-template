package com.springernature.http4k.template

import com.springernature.http4k.config.StackSettings.PORT
import org.http4k.cloudnative.env.Environment
import org.http4k.cloudnative.env.EnvironmentKey
import org.http4k.cloudnative.env.Port
import org.http4k.lens.int

object Settings {
    private val DEFAULT_PORT= Port(8080)

    val defaults = Environment.defaults(PORT of DEFAULT_PORT)

    val AMOUNT_OF_CAKES = EnvironmentKey.int().defaulted("AMOUNT_OF_CAKES", 2)
}

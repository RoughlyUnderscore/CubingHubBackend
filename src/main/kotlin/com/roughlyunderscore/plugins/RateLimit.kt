package com.roughlyunderscore.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.*
import org.slf4j.event.*
import kotlin.time.Duration.Companion.seconds

fun Application.configureRateLimit() {
  install(RateLimit) {
    register {
      rateLimiter(limit = 75, refillPeriod = 60.seconds)
    }
  }
}

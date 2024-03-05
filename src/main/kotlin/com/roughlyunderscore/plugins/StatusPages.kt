package com.roughlyunderscore.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.slf4j.event.*
import kotlin.time.Duration.Companion.seconds

fun Application.configureStatusPages() {
  install(StatusPages) {
    status(HttpStatusCode.TooManyRequests) { call, status ->
      val retryAfter = call.response.headers[HttpHeaders.RetryAfter]
      call.respond(status, "Rate limit exceeded. Try again in $retryAfter.")
    }

    status(HttpStatusCode.MovedPermanently) { call, status ->
      call.respond(status, "This page has moved to a new location.")
    }

    status(HttpStatusCode.TemporaryRedirect) { call, status ->
      call.respond(status, "This page has moved temporarily to a new location.")
    }

    status(HttpStatusCode.PermanentRedirect) { call, status ->
      call.respond(status, "This page has moved permanently to a new location.")
    }
  }
}

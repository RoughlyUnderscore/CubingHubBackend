package com.roughlyunderscore.plugins

import com.roughlyunderscore.plugins.routes.auth.*
import com.roughlyunderscore.plugins.routes.rate.rate
import com.roughlyunderscore.plugins.routes.rate.unrate
import com.roughlyunderscore.plugins.routes.subsets.subsetAlgorithms
import com.roughlyunderscore.plugins.routes.subsets.subsets
import io.ktor.server.application.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
  routing {
    rateLimit {
      get("/") {
        val requestsLeft = call.response.headers["X-RateLimit-Remaining"]
        call.respondText("$requestsLeft left (refills every minute)")
      }

      // default path is api/v1
      route("/api/v1") {
        get("/") {
          val requestsLeft = call.response.headers["X-RateLimit-Remaining"]
          call.respondText("$requestsLeft left (refills every minute)")
        }

        subsets()
        subsetAlgorithms()
        register()
        auth()
        logout()
        changePassword()
        rate()
        unrate()
        delete()
        verifyToken()
      }
    }
  }
}
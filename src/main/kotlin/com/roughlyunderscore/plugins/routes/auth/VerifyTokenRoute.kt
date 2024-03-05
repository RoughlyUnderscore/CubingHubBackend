// Copyright 2024 RoughlyUnderscore
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,git
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.roughlyunderscore.plugins.routes.auth

import com.roughlyunderscore.data.model.fullValidateFetchPayload
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.coroutineScope

fun Route.verifyToken() {
  get("/verify_token") {
    val token = call.parameters["token"] ?: return@get call.respond(HttpStatusCode.BadRequest, "No token specified")

    coroutineScope {
      val validationPair = fullValidateFetchPayload(token)
      if (validationPair.second != null) {
        return@coroutineScope call.respond(validationPair.second!!.first, validationPair.second!!.second)
      }

      val payload = validationPair.first ?: return@coroutineScope call.respond(HttpStatusCode.InternalServerError, "Failed to validate token")
      val email = payload.subject
      val id = payload.id

      call.respond(HttpStatusCode.OK, object {
        val id = id
        val email = email
      })
    }
  }
}
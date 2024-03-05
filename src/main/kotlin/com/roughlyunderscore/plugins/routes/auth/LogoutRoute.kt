// Copyright 2024 RoughlyUnderscore
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.roughlyunderscore.plugins.routes.auth

import com.roughlyunderscore.accountDb
import com.roughlyunderscore.data.model.fullValidateFetchId
import com.roughlyunderscore.ch.lib.INVALIDATE_TOKEN
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.coroutineScope

fun Route.logout() {
  get("/logout") {
    coroutineScope {
      val token = call.parameters["token"] ?: return@coroutineScope call.respond(HttpStatusCode.Unauthorized, "No token specified")

      // Validate token
      val validation = fullValidateFetchId(token)
      if (validation.second != null) {
        return@coroutineScope call.respond(validation.second!!.first, validation.second!!.second)
      }

      // Add a token to the table of invalid tokens
      accountDb.update(INVALIDATE_TOKEN, token)

      call.respond(HttpStatusCode.OK, object {
        val loggedOut = true
      })
    }
  }
}
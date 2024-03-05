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
import com.roughlyunderscore.ch.lib.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.coroutineScope

fun Route.register() {
  get("/register") {
    coroutineScope {
      val email = call.parameters["email"] ?: return@coroutineScope call.respond(HttpStatusCode.NotFound, "No email specified")
      val password = call.parameters["password"] ?: return@coroutineScope call.respond(HttpStatusCode.NotFound, "No password specified")
      val confirmPrivacyPolicy = call.parameters["agree_to_privacy_policy"]?.lowercase()
      val confirmTermsOfService = call.parameters["agree_to_terms_of_service"]?.lowercase()

      if (confirmPrivacyPolicy != "true" || confirmTermsOfService != "true") {
        return@coroutineScope call.respond(HttpStatusCode.Forbidden, "Privacy policy and terms of service must be confirmed")
      }

      if (!isValidEmail(email)) {
        return@coroutineScope call.respond(HttpStatusCode.BadRequest, "Invalid email")
      }

      if (!isSecurePassword(password)) {
        return@coroutineScope call.respond(HttpStatusCode.BadRequest, "Password is not secure enough")
      }

      val response = accountDb.query(FETCH_ACCOUNT_BY_MAIL, listOf(email)) {
        if (it.next()) return@query (HttpStatusCode.Conflict to "Account already exists")

        val hash = hash(password)
        accountDb.update(ADD_ACCOUNT, email, hash)
        return@query (HttpStatusCode.OK to "Account created")
      }

      call.respond(response.first, response.second)
    }
  }
}
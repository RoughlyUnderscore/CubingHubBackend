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

package com.roughlyunderscore.data.model

import com.roughlyunderscore.accountDb
import com.roughlyunderscore.tokenSecret
import io.jsonwebtoken.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

private const val FIND_INVALID_TOKEN = "select * from invalid_tokens where token = ? limit 1;"
private const val FIND_ACCOUNT_BY_EMAIL = "select * from accounts where email = ? limit 1;"

@Serializable
data class Token(val token: String)

suspend fun generateToken(email: String): Token {
  return accountDb.query(FIND_ACCOUNT_BY_EMAIL, listOf(email)) { accountSet ->
    if (!accountSet.next()) throw Exception("Account not found")

    val id = accountSet.getInt("id")

    Instant.now().let { instant ->
      Token(
        Jwts.builder()
          .subject(email)
          .id(id.toString())
          .issuedAt(Date.from(instant))
          .expiration(Date.from(instant.plus(14, ChronoUnit.DAYS)))
          .signWith(tokenSecret, Jwts.SIG.HS512)
          .compact()
      )
    }
  }
}

/**
 * @return whether the token is valid
 */
fun validateToken(token: String): Jws<Claims>? = try {
  Jwts.parser().verifyWith(tokenSecret).build().parseSignedClaims(token)
} catch (e: Exception) {
  null
}

/**
 * @return Pair(nullable id, nullable Pair(status code, error message))
 */
suspend fun fullValidateFetchId(token: String): Pair<Int?, Pair<HttpStatusCode, String>?> {
  // 1) Validate the token
  val validatedToken = validateToken(token) ?: return null to (HttpStatusCode.Unauthorized to "Invalid token")

  // 2) Check if the token is in the invalid_tokens table
  if (accountDb.query(FIND_INVALID_TOKEN, listOf(token)) {
    if (it.next()) return@query (HttpStatusCode.Unauthorized to "Invalid token")
    else return@query null
  } != null) return null to (HttpStatusCode.InternalServerError to "Failed to verify the token's validity")

  // 3) Make sure that the last password change happened before the token was issued
  val payload = validatedToken.payload
  val issuedAt = payload.issuedAt.time
  val subject = payload.subject

  return accountDb.query(FIND_ACCOUNT_BY_EMAIL, listOf(subject)) {
    if (!it.next()) return@query (HttpStatusCode.NotFound to "Account not found")

    val id = it.getInt("id")
    val lastPasswordChange = it.getLong("last_pw_change")

    if (lastPasswordChange > issuedAt) return@query (HttpStatusCode.Unauthorized to "Outdated token")
    else return@query (HttpStatusCode.OK to id.toString())
  }.let {
    if (!it.first.isSuccess()) return@let null to (it.first to it.second)
    else return it.second.toInt() to null
  }
}

/**
 * @return Pair(nullable string, nullable Pair(status code, error message))
 */
suspend fun fullValidateFetchEmail(token: String): Pair<String?, Pair<HttpStatusCode, String>?> {
  // 1) Validate the token
  val validatedToken = validateToken(token) ?: return null to (HttpStatusCode.Unauthorized to "Invalid token")

  // 2) Check if the token is in the invalid_tokens table
  if (accountDb.query(FIND_INVALID_TOKEN, listOf(token)) {
      if (it.next()) return@query (HttpStatusCode.Unauthorized to "Invalid token")
      else return@query null
    } != null) return null to (HttpStatusCode.InternalServerError to "Failed to verify the token's validity")

  // 3) Make sure that the last password change happened before the token was issued
  val payload = validatedToken.payload
  val issuedAt = payload.issuedAt.time
  val subject = payload.subject

  return accountDb.query(FIND_ACCOUNT_BY_EMAIL, listOf(subject)) {
    if (!it.next()) return@query (HttpStatusCode.NotFound to "Account not found")

    val lastPasswordChange = it.getLong("last_pw_change")

    if (lastPasswordChange > issuedAt) return@query (HttpStatusCode.Unauthorized to "Outdated token")
    else return@query (HttpStatusCode.OK to subject)
  }.let {
    if (!it.first.isSuccess()) return@let null to (it.first to it.second)
    else return it.second to null
  }
}

/**
 * @return Pair(nullable claims, nullable Pair(status code, error message))
 */
suspend fun fullValidateFetchPayload(token: String): Pair<Claims?, Pair<HttpStatusCode, String>?> {
  // 1) Validate the token
  val validatedToken = validateToken(token) ?: return null to (HttpStatusCode.Unauthorized to "Invalid token")

  // 2) Check if the token is in the invalid_tokens table
  if (accountDb.query(FIND_INVALID_TOKEN, listOf(token)) {
      if (it.next()) return@query (HttpStatusCode.Unauthorized to "Invalid token")
      else return@query null
    } != null) return null to (HttpStatusCode.InternalServerError to "Failed to verify the token's validity")

  // 3) Make sure that the last password change happened before the token was issued
  val payload = validatedToken.payload
  val issuedAt = payload.issuedAt.time
  val subject = payload.subject

  return accountDb.query(FIND_ACCOUNT_BY_EMAIL, listOf(subject)) {
    if (!it.next()) return@query (HttpStatusCode.NotFound to "Account not found")

    val lastPasswordChange = it.getLong("last_pw_change")
    if (lastPasswordChange > issuedAt) return@query (HttpStatusCode.Unauthorized to "Outdated token")
    else return@query (HttpStatusCode.OK to null)
  }.let {
    if (!it.first.isSuccess()) return@let null to (it.first to it.second!!)
    else return payload to null
  }
}
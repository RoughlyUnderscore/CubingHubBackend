package com.roughlyunderscore

import com.roughlyunderscore.ch.lib.*
import com.roughlyunderscore.db.Database
import com.roughlyunderscore.plugins.configureMonitoring
import com.roughlyunderscore.plugins.configureRateLimit
import com.roughlyunderscore.plugins.configureRouting
import com.roughlyunderscore.plugins.configureSerialization
import com.roughlyunderscore.plugins.configureStatusPages
import io.github.cdimascio.dotenv.Dotenv
import io.jsonwebtoken.Jwts
import io.ktor.server.application.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking
import javax.crypto.SecretKey

lateinit var db: Database
lateinit var accountDb: Database

val tokenSecret: SecretKey = Jwts.SIG.HS512.key().build()

fun main(args: Array<String>) {
  init(args)
}

private fun init(args: Array<String>) {
  val dotenv = Dotenv.load()

  // I have no idea why but Dotenv does not strip the leading and trailing apostrophe character on strings.
  val url = dotenv["DATABASE_URL"].let { it.slice(1 .. it.length - 2) }
  val port = dotenv["DATABASE_PORT"].let { it.slice(1 .. it.length - 2) }
  val database = dotenv["DATABASE_NAME"].let { it.slice(1 .. it.length - 2) }
  val username = dotenv["DATABASE_USERNAME"].let { it.slice(1 .. it.length - 2) }
  val password = dotenv["DATABASE_PASSWORD"].let { it.slice(1 .. it.length - 2) }

  val accountUrl = dotenv["ACCOUNT_DATABASE_URL"].let { it.slice(1 .. it.length - 2) }
  val accountPort = dotenv["ACCOUNT_DATABASE_PORT"].let { it.slice(1 .. it.length - 2) }
  val accountDatabase = dotenv["ACCOUNT_DATABASE_NAME"].let { it.slice(1 .. it.length - 2) }
  val accountUsername = dotenv["ACCOUNT_DATABASE_USERNAME"].let { it.slice(1 .. it.length - 2) }
  val accountPassword = dotenv["ACCOUNT_DATABASE_PASSWORD"].let { it.slice(1 .. it.length - 2) }

  db = Database(url, port, database, username, password)
  accountDb = Database(accountUrl, accountPort, accountDatabase, accountUsername, accountPassword)

  runBlocking {
    db.update(CREATE_ALGS_TABLE)
    db.update(CREATE_SUBSETS_TABLE)
    db.update(CREATE_VARIATIONS_TABLE)

    accountDb.update(CREATE_ACCOUNTS_TABLE)
    accountDb.update(CREATE_INVALID_TOKENS_TABLE)
  }

  EngineMain.main(args)
}

fun Application.module() {
  configureRateLimit()
  configureStatusPages()
  configureMonitoring()
  configureSerialization()
  configureRouting()
}

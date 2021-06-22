package com.example.plugins

import com.example.modul.DataExample
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*

fun Application.configureRouting() {
    install(DoubleReceive)
    install(KtorServerLogging)
    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        post("/double-receive") {
            val first = call.receiveText()
            val theSame = call.receiveText()
            call.respondText("$first $theSame")
        }
        post("/json/jackson") {
            val request = call.receive<DataExample>()
            val client = HttpClient(Apache) {
                install(JsonFeature) {
                    serializer = JacksonSerializer()
                }
                install(KtorClientLogging)
            }
            val httpResponse: HttpResponse = client.post("https://jsonplaceholder.typicode.com/posts") {
                contentType(ContentType.Application.Json)
                body = request
            }
            val response = httpResponse.receive<DataExample>()
            call.respond(response)
        }
    }
}

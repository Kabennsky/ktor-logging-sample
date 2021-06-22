package com.example.plugins

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.features.*
import io.ktor.client.features.observer.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.util.*
import io.ktor.utils.io.*
import io.ktor.utils.io.charsets.*
import io.ktor.utils.io.core.*
import org.slf4j.LoggerFactory

class KtorClientLogging(
    private val logger: org.slf4j.Logger
) {
    class Config {
        var logger: org.slf4j.Logger = LoggerFactory.getLogger(KtorClientLogging::class.java)
    }

    private fun logRequest(request: HttpRequestBuilder) {
        val content = when (request.body) {
            is TextContent -> (request.body as TextContent).text
            else -> request.body.toString()
        }
        logger.info("client request {}", content)
    }

    private suspend fun logResponse(call: HttpClientCall): HttpResponse {
        val (loggingContent, processingContent) = call.response.content.split(call.response)
        val processingCall = call.wrapWithContent(processingContent)
        val loggingResponse = processingCall.wrapWithContent(loggingContent).response
        val body = loggingResponse.content.tryReadText(loggingResponse.contentType()?.charset())
        logger.info("client response {}", body)
        return processingCall.response
    }

    companion object : HttpClientFeature<Config, KtorClientLogging> {
        override val key: AttributeKey<KtorClientLogging> = AttributeKey("ClientLogging")

        override fun prepare(block: Config.() -> Unit): KtorClientLogging {
            val config = Config().apply(block)
            return KtorClientLogging(config.logger)
        }

        override fun install(feature: KtorClientLogging, scope: HttpClient) {
            scope.sendPipeline.intercept(HttpSendPipeline.Monitoring) {
                feature.logRequest(context)
                proceedWith(subject)
            }

            scope.receivePipeline.intercept(HttpReceivePipeline.After) {
                val response = feature.logResponse(context)
                proceedWith(response)
            }
        }
    }

    private suspend inline fun ByteReadChannel.tryReadText(charset: Charset?): String =
        runCatching { readRemaining().readText(charset = charset ?: Charsets.UTF_8) }.getOrDefault("content omitted")
}

package com.example.plugins

import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.util.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class KtorServerLogging(private val logger: Logger) {

    class Configuration {
        var logger: Logger = LoggerFactory.getLogger(KtorServerLogging::class.java)
    }

    private suspend fun logRequest(call: ApplicationCall) {
        logger.info("server request {}", String(call.receive<ByteArray>()))
    }

    private fun logResponse(call: ApplicationCall, subject: Any) {
        // ToDo: get response body from subject, but subject ist of Type OutputStreamContent
        logger.info("server response {}", "ToDo")
    }

    /**
     * Feature installation.
     */
    fun install(pipeline: Application) {
        pipeline.intercept(ApplicationCallPipeline.Monitoring) {
            logRequest(call)
        }
        pipeline.sendPipeline.intercept(ApplicationSendPipeline.After) {
            logResponse(call, subject)
        }
    }

    companion object Feature : ApplicationFeature<Application, Configuration, KtorServerLogging> {

        override val key = AttributeKey<KtorServerLogging>("Logging Feature")

        override fun install(pipeline: Application, configure: Configuration.() -> Unit): KtorServerLogging {
            val configuration = Configuration().apply(configure)

            return KtorServerLogging(configuration.logger).apply { install(pipeline) }
        }
    }
}
package moe.tachyon.route

import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.github.smiley4.ktorswaggerui.routing.openApiSpec
import io.github.smiley4.ktorswaggerui.routing.swaggerUI
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.router() = routing()
{
    val rootPath = this.application.rootPath

    get("/", { hidden = true })
    {
        call.respondRedirect("$rootPath/api-docs")
    }

    authenticate("auth-api-docs")
    {
        route("/api-docs")
        {
            route("/api.json")
            {
                openApiSpec()
            }
            swaggerUI("$rootPath/api-docs/api.json")
        }
    }
    
    // todo the routes which don't need to be authenticated
    
    authenticate("auth", optional = true)
    {
        install(createRouteScopedPlugin("ProhibitPlugin", { })
        {
            onCall {
                // todo check the user whether was banned or not
            }
        })
        
        // todo the routes which need to be authenticated
    }
}
package com.wpf.server.plugins

import com.wpf.server.serverBasePath
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File

fun Application.configureRouting() {
    routing {
        get("/getFile{filePath?}") {
            val filePath = call.parameters["filePath"] ?: return@get call.respondText("请输入文件路径")
            if (serverBasePath.isEmpty()) return@get call.respondText("请配置基础目录")
            runCatching {
                val file = File(serverBasePath + File.separator + filePath)
                if (!file.exists()) {
                    call.respondText("文件未找到")
                    return@get
                }
                call.response.header(
                    HttpHeaders.ContentDisposition,
                    ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, file.name).toString()
                )
                call.respondFile(file)
            }.onFailure {
                call.respondText("文件未找到")
            }
        }
    }
}

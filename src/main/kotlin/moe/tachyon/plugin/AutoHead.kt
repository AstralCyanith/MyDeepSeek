@file:Suppress("PackageDirectoryMismatch")

package moe.tachyon.plugin.autoHead

import io.ktor.server.application.*
import io.ktor.server.plugins.autohead.*

fun Application.installAutoHead() = install(AutoHeadResponse)
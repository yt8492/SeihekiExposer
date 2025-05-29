plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.shadow)
  application
}

application {
  mainClass.set("io.ktor.server.netty.EngineMain")
}

dependencies {
  implementation(libs.ktor.server.netty)
  implementation(libs.ktor.server.websockets)
}

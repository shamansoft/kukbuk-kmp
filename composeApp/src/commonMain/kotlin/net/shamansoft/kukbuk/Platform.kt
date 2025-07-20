package net.shamansoft.kukbuk

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
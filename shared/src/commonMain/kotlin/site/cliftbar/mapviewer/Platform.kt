package site.cliftbar.mapviewer

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
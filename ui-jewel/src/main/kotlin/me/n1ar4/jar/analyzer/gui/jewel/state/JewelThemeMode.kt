package me.n1ar4.jar.analyzer.gui.jewel.state

enum class JewelThemeMode {
    ISLAND_LIGHT,
    ISLAND_DARK,
    ;

    companion object {
        fun fromRuntimeTheme(theme: String?): JewelThemeMode {
            return if (theme?.trim()?.lowercase() == "dark") ISLAND_DARK else ISLAND_LIGHT
        }
    }
}

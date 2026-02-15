package me.n1ar4.jar.analyzer.gui.jewel.state

enum class CoreTab(val index: Int, val title: String) {
    START(0, "start"),
    SEARCH(1, "search"),
    CALL(2, "call"),
    IMPL(3, "impl"),
    WEB(4, "web"),
    NOTE(5, "note"),
    SCA(6, "sca"),
    LEAK(7, "leak"),
    GADGET(8, "gadget"),
    ADVANCE(9, "advance"),
    CHAINS(10, "chains"),
    API(11, "API");

    companion object {
        fun fromIndex(index: Int): CoreTab {
            return entries.firstOrNull { it.index == index } ?: START
        }
    }
}

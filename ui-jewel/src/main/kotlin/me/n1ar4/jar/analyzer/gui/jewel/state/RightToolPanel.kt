package me.n1ar4.jar.analyzer.gui.jewel.state

enum class RightToolPanel(
    val title: String,
    val coreTab: CoreTab?,
    val bottomStripe: Boolean = false,
) {
    START("start", CoreTab.START),
    SEARCH("search", CoreTab.SEARCH),
    CALL("call", CoreTab.CALL),
    IMPL("impl", CoreTab.IMPL),
    WEB("web", CoreTab.WEB),
    NOTE("note", CoreTab.NOTE),
    SCA("sca", CoreTab.SCA),
    LEAK("leak", CoreTab.LEAK),
    GADGET("gadget", CoreTab.GADGET),
    ADVANCE("advance", CoreTab.ADVANCE),
    CHAINS("chains", CoreTab.CHAINS),
    API("API", CoreTab.API),
    METHODS("methods", null, bottomStripe = true),
    LOG("log", null, bottomStripe = true),
    ;

    companion object {
        val topStripePanels: List<RightToolPanel> = entries.filter { !it.bottomStripe }
        val bottomStripePanels: List<RightToolPanel> = entries.filter { it.bottomStripe }

        fun fromCoreTab(tab: CoreTab): RightToolPanel {
            return entries.firstOrNull { it.coreTab == tab } ?: START
        }
    }
}

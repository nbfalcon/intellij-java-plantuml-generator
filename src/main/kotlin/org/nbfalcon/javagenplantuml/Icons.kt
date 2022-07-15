package org.nbfalcon.javagenplantuml

import com.intellij.ui.IconManager
import javax.swing.Icon

object Icons {
    private fun load(@Suppress("SameParameterValue") path: String): Icon {
        return IconManager.getInstance().getIcon(path, this.javaClass)
    }

    @JvmField
    val PlantUML = load("icons/PlantUML.svg")
}
package org.nbfalcon.javagenplantuml.actions

import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.psi.PsiJavaFile
import org.nbfalcon.javagenplantuml.javaPsi2UML
import java.awt.datatransfer.StringSelection

class GenUMLAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val clazz = getPsiClass(e) ?: return
        val uml = javaPsi2UML(clazz)
        val src = uml.toPlantUML()
        CopyPasteManager.getInstance().setContents(StringSelection(src))
    }

    private fun getPsiClass(e: AnActionEvent) =
        (e.getData(CommonDataKeys.PSI_FILE) as? PsiJavaFile)?.classes?.firstOrNull()

    override fun update(e: AnActionEvent) {
        super.update(e)
        val enabled = getPsiClass(e) != null
        e.presentation.isEnabled = enabled
        e.presentation.isVisible = enabled || !ActionPlaces.isPopupPlace(e.place)
    }
}
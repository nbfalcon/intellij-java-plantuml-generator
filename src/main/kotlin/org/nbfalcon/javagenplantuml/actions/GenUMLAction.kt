package org.nbfalcon.javagenplantuml.actions

import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiPackage
import com.intellij.psi.impl.file.PsiJavaDirectoryImpl
import com.intellij.psi.util.parentOfType
import org.nbfalcon.javagenplantuml.UMLClass
import org.nbfalcon.javagenplantuml.javaPsi2UML
import org.nbfalcon.javagenplantuml.util.traverseDfs
import java.awt.datatransfer.StringSelection

class GenUMLAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val elements = getElementsToConsider(e)

        val umls = mutableListOf<UMLClass>()
        val alreadyVisited = mutableSetOf<PsiElement>()

        traverseDfs(elements.toTypedArray()) { elem ->
            if (!alreadyVisited.add(elem)) {
                return@traverseDfs null
            }

            val classesNow = when (elem) {
                is PsiClass -> arrayOf(elem)
                is PsiJavaFile -> elem.classes
                is PsiPackage -> elem.classes
                else -> null
            }
            classesNow?.forEach {
                val uml = javaPsi2UML(it)
                umls.add(uml)
            }

            when (elem) {
                is PsiPackage -> elem.subPackages
                is PsiJavaDirectoryImpl -> elem.files.filterIsInstance<PsiJavaFile>().toTypedArray<PsiElement>() + elem.subdirectories
                else -> null
            }
        }

        val umlS = umls.joinToString("\n") { uml -> uml.toPlantUML() }
        val src = "@startuml\n\n$umlS\n@enduml\n"

        CopyPasteManager.getInstance().setContents(StringSelection(src))
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        val enabled = getElementsToConsider(e).isNotEmpty()
        e.presentation.isEnabled = enabled
        e.presentation.isVisible = enabled || !ActionPlaces.isPopupPlace(e.place)
    }

    private fun getElementsToConsider(e: AnActionEvent): List<PsiElement> {
        val elements = e.getData(LangDataKeys.PSI_ELEMENT_ARRAY) ?: arrayOf()
        val javaElements =
            elements.filter { element -> element is PsiJavaFile || element is PsiPackage || element is PsiJavaDirectoryImpl || element is PsiClass }

        return javaElements.ifEmpty {
            val elem = e.getData(CommonDataKeys.PSI_ELEMENT)?.parentOfType<PsiClass>(true)
            if (elem != null) listOf(elem) else listOf()
        }
    }
}
package org.nbfalcon.javagenplantuml.actions

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiPackage
import com.intellij.psi.impl.file.PsiJavaDirectoryImpl
import com.intellij.psi.util.parentOfType
import com.twelvemonkeys.imageio.stream.ByteArrayImageInputStream
import net.sourceforge.plantuml.FileFormat
import net.sourceforge.plantuml.FileFormatOption
import net.sourceforge.plantuml.SourceStringReader
import org.nbfalcon.javagenplantuml.UMLClass
import org.nbfalcon.javagenplantuml.javaPsi2UML
import org.nbfalcon.javagenplantuml.util.ImageSelection
import org.nbfalcon.javagenplantuml.util.traverseDfs
import java.awt.datatransfer.StringSelection
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

abstract class PlantUMLActionBase : AnAction() {
    fun generatePlantUML(e: AnActionEvent): String {
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
                is PsiJavaDirectoryImpl -> elem.files.filterIsInstance<PsiJavaFile>()
                    .toTypedArray<PsiElement>() + elem.subdirectories
                else -> null
            }
        }

        val umlS = umls.joinToString("\n") { uml -> uml.toPlantUML() }

        return "@startuml\n\n$umlS\n@enduml\n"
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

class GenUMLAction : PlantUMLActionBase() {
    override fun actionPerformed(e: AnActionEvent) {
        CopyPasteManager.getInstance().setContents(StringSelection(generatePlantUML(e)))
    }
}

class GenImageAction : PlantUMLActionBase() {
    override fun actionPerformed(e: AnActionEvent) {
        val src = generatePlantUML(e)

        val out = ByteArrayOutputStream()
        SourceStringReader(src).generateImage(out, FileFormatOption(FileFormat.PNG))

        if (out.size() != 0) {
            val image = ImageIO.read(ByteArrayImageInputStream(out.toByteArray()))
            CopyPasteManager.getInstance().setContents(ImageSelection(image))
        }
    }
}
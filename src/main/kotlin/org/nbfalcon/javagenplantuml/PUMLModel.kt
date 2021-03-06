package org.nbfalcon.javagenplantuml

import java.util.stream.Collectors

interface UMLElem {
    fun toPlantUML(): String
}

fun StringBuilder.appendWithSpace(appendMe: String): StringBuilder =
    if (isNotEmpty()) {
        append(' ').append(appendMe)
    } else {
        append(appendMe)
    }

class UMLAttribute(
    private val identifier: String, private val access: Access,
    private val isStatic: Boolean, private val isAbstract: Boolean, private val isFinal: Boolean
) : UMLElem {
    enum class Access {
        PUBLIC, PRIVATE, PACKAGE_PRIVATE, PROTECTED
    }

    override fun toPlantUML(): String {
        val modifier = StringBuilder()

        if (isStatic) modifier.appendWithSpace("{static}")
        if (isAbstract) modifier.appendWithSpace("{abstract}")

        val accessS = when (access) {
            Access.PUBLIC -> "+"
            Access.PRIVATE -> "-"
            Access.PACKAGE_PRIVATE -> "~"
            Access.PROTECTED -> "#"
        }

        var lhs = accessS + modifier + identifier

        if (isFinal) lhs += " {readOnly}"

        return lhs
    }
}

class UMLRelation(
    private val destClass: String,
    private val name: String,
    private val lhs: Multiplicity,
    private val rhs: Multiplicity
) {
    sealed class Multiplicity private constructor() : UMLElem {
        object N : Multiplicity() {
            override fun toPlantUML(): String = "\"N\""
        }

        class C(private val n: Int) : Multiplicity() {
            override fun toPlantUML(): String = "\"$n\""
        }
    }

    fun toPlantUML(srcClass: String): String {
        return "\"$srcClass\" ${lhs.toPlantUML()} --> ${rhs.toPlantUML()} \"$destClass\" : \"$name\""
    }
}

class UMLInheritance(private val className: String, private val type: InheritanceType) {
    enum class InheritanceType {
        ABSTRACT, EXTENDS
    }

    fun toPlantUML(srcClass: String): String {
        val arrow = when (type) {
            InheritanceType.ABSTRACT -> "<|.."
            InheritanceType.EXTENDS -> "<|--"
        }
        return "\"$className\" $arrow \"$srcClass\""
    }
}

class UMLClass(
    private val name: String,
    private val classType: ClassType,
    private val attributes: List<UMLAttribute>,
    private val methods: List<UMLAttribute>,
    private val relations: List<UMLRelation>,
    private val inherits: List<UMLInheritance>
) : UMLElem {
    enum class ClassType {
        CLASS, ABSTRACT_CLASS, INTERFACE, ENUM
    }

    override fun toPlantUML(): String {
        val prefix = when (classType) {
            ClassType.CLASS -> "class"
            ClassType.ABSTRACT_CLASS -> "abstract class"
            ClassType.INTERFACE -> "interface"
            ClassType.ENUM -> "enum"
        }

        var attributesS = attributes.stream().map { attr -> "\t" + attr.toPlantUML() }.collect(Collectors.joining("\n"))
        if (attributes.isNotEmpty()) attributesS += "\n"
        var methodsS = methods.stream().map { attr -> "\t" + attr.toPlantUML() }.collect(Collectors.joining("\n"))
        if (methods.isNotEmpty()) methodsS += "\n"

        var relationsS = relations.stream().map { r -> r.toPlantUML(name) }.collect(Collectors.joining("\n"))
        var inheritanceS = inherits.stream().map { i -> i.toPlantUML(name) }.collect(Collectors.joining("\n"))

        if (relationsS.isNotEmpty()) relationsS = "\n" + relationsS
        if (inheritanceS.isNotEmpty()) inheritanceS = "\n" + inheritanceS

        return "$prefix \"$name\" {\n$attributesS$methodsS}$inheritanceS$relationsS\n"
    }
}
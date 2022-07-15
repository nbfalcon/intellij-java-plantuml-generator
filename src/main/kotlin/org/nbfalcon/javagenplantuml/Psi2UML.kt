package org.nbfalcon.javagenplantuml

import com.intellij.psi.*
import com.intellij.psi.PsiModifier.ModifierConstant

val PsiModifierListOwner.visibility
    get() = modifierList?.let { mL ->
        when {
            mL.hasModifierProperty(PsiModifier.PUBLIC) -> UMLAttribute.Access.PUBLIC
            mL.hasModifierProperty(PsiModifier.PRIVATE) -> UMLAttribute.Access.PRIVATE
            mL.hasModifierProperty(PsiModifier.PROTECTED) -> UMLAttribute.Access.PROTECTED
            else -> null
        }
    } ?: UMLAttribute.Access.PACKAGE_PRIVATE

val PsiMethod.declarationSignature: String
    get() {
        val generics =
            if (typeParameters.isNotEmpty()) "<" + typeParameters.joinToString(", ") { p -> p.name!! } + ">"
            else ""
        val args = parameters.joinToString(", ") { p -> (p.type as PsiType).presentableText + " " + p.name!! }
        val ret = returnType?.let { rT -> rT.presentableText + " " } ?: ""

        return "$ret$name$generics($args)"
    }

fun PsiModifierListOwner.hasProperty(@ModifierConstant property: String) =
    modifierList?.hasModifierProperty(property) == true

fun convertMember(m: PsiMember): UMLAttribute {
    val visibility = m.visibility

    val text = when (m) {
        is PsiMethod -> m.declarationSignature
        is PsiField -> m.type.presentableText + " " + m.name
        else -> throw IllegalArgumentException("$m is not a method or field")
    }

    return UMLAttribute(
        text, visibility,
        isStatic = m.hasProperty(PsiModifier.STATIC),
        isAbstract = m.hasProperty(PsiModifier.ABSTRACT),
        isFinal = m.hasProperty(PsiModifier.FINAL)
    )
}

val PsiArrayType.deArray: PsiType
    get() {
        var a: PsiType = this
        while (a is PsiArrayType) {
            a = a.componentType
        }
        return a
    }

val PsiType.qualifiedNameWithoutGenerics: String
    get() {
        val p = canonicalText.removePrefix("java.lang.")
        val generics = p.indexOf('<')
        return if (generics != -1) p.substring(0, generics)
        else p
    }

fun convertRelationalField(m: PsiField): UMLRelation {
    return when (val ty = m.type) {
        is PsiArrayType -> UMLRelation(
            ty.deArray.qualifiedNameWithoutGenerics, m.name, UMLRelation.Multiplicity.C(1), UMLRelation.Multiplicity.N
        )
        else -> UMLRelation(
            ty.qualifiedNameWithoutGenerics,
            m.name,
            UMLRelation.Multiplicity.C(1),
            UMLRelation.Multiplicity.C(1)
        )
    }
}

/**
 * Just a field and not a relation.
 */
fun isBasicField(field: PsiField): Boolean =
    field.type is PsiPrimitiveType || field.type.canonicalText.startsWith("java.lang.")
            || field.modifierList?.hasModifierProperty(PsiModifier.STATIC) == true

val PsiClass.qualifiedNameWithoutGenerics: String?
    get() = qualifiedName?.let { qn ->
        val generics = qn.indexOf('<')

        if (generics != -1) qn.substring(0, generics)
        else qn
    }

fun convertInheritance(clazz: PsiClass): UMLInheritance {
    val type = if (clazz.isInterface || clazz.modifierList?.hasModifierProperty(PsiModifier.ABSTRACT) == true) {
        UMLInheritance.InheritanceType.ABSTRACT
    } else UMLInheritance.InheritanceType.EXTENDS
    return UMLInheritance(clazz.qualifiedNameWithoutGenerics!!, type)
}

val PsiClass.classType: UMLClass.ClassType
    get() = when {
        isEnum -> UMLClass.ClassType.ENUM
        isInterface -> UMLClass.ClassType.INTERFACE
        else -> {
            if (modifierList?.hasModifierProperty(PsiModifier.ABSTRACT) == true) UMLClass.ClassType.ABSTRACT_CLASS
            else UMLClass.ClassType.CLASS
        }
    }

fun javaPsi2UML(clazz: PsiClass): UMLClass {
    val fields = clazz.fields.filter(::isBasicField).map(::convertMember)
    val relations = clazz.fields.filter { !isBasicField(it) }.map { convertRelationalField(it) }

    val methods = clazz.methods.map(::convertMember)

    val inherits = ((clazz.extendsListTypes ?: arrayOf()) + (clazz.implementsListTypes ?: arrayOf()))
        .map { rT -> convertInheritance(rT.resolve()!!) }

    return UMLClass(clazz.qualifiedName!!, clazz.classType, fields, methods, relations, inherits)
}
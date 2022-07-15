package org.nbfalcon.javagenplantuml

import com.intellij.psi.*

val PsiType.presentableCanonicalText: String
    get() = canonicalText.removePrefix("java.lang.")

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
        val args = parameters.joinToString(", ") { p -> (p.type as PsiType).presentableCanonicalText + " " + p.name!! }
        val ret = returnType?.let { rT -> rT.presentableCanonicalText + " " } ?: ""

        return "$ret$name$generics($args)"
    }

fun convertMember(m: PsiMember): UMLAttribute {
    val visibility = m.visibility

    val modifier = m.modifierList?.let { mL ->
        when {
            mL.hasModifierProperty(PsiModifier.STATIC) -> UMLAttribute.Modifier.STATIC
            mL.hasModifierProperty(PsiModifier.ABSTRACT) -> UMLAttribute.Modifier.ABSTRACT
            else -> null
        }
    } ?: UMLAttribute.Modifier.NONE

    val text = when (m) {
        is PsiMethod -> m.declarationSignature
        is PsiField -> m.type.presentableCanonicalText + " " + m.name
        else -> throw IllegalArgumentException("$m is not a method or field")
    }

    return UMLAttribute(visibility, modifier, text)
}

val PsiArrayType.deArray: PsiType
    get() {
        var a: PsiType = this
        while (a is PsiArrayType) {
            a = a.componentType
        }
        return a
    }

fun convertRelationalField(m: PsiField): UMLRelation {
    return when (val ty = m.type) {
        is PsiArrayType -> UMLRelation(
            ty.deArray.presentableCanonicalText, m.name, UMLRelation.Multiplicity.C(1), UMLRelation.Multiplicity.N
        )
        else -> UMLRelation(
            ty.presentableCanonicalText,
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

fun convertInheritance(clazz: PsiClass): UMLInheritance {
    return if (clazz.isInterface || clazz.modifierList?.hasModifierProperty(PsiModifier.ABSTRACT) == true) {
        UMLInheritance(clazz.qualifiedName!!, UMLInheritance.InheritanceType.ABSTRACT)
    } else {
        UMLInheritance(clazz.qualifiedName!!, UMLInheritance.InheritanceType.EXTENDS)
    }
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
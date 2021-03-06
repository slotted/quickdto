package org.reladev.quickdto.processor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import org.reladev.quickdto.shared.QuickCopy;
import org.reladev.quickdto.shared.QuickDto;

import static org.reladev.quickdto.processor.QuickDtoProcessor.*;

public class Type {
    private TypeMirror typeMirror;
    private TypeKind typeKind;
    private Type collectionType;
    private ArrayList<Type> genericTypes;

    private String originalName;
    private String name;
    private String packageString;
    private String qualifiedName;

    private boolean isQuickDto;
    private boolean isQuickDtoList;
    private boolean isQuickCopy;

    //transient
    private String primitiveBoxType;

    public Type(String packageString, String name) {
        this.name = name;
        this.packageString = packageString;
    }

    protected Type(Class type) {
        originalName = type.getSimpleName();
        name = type.getSimpleName();
        packageString = type.getPackage()
                            .getName();
        qualifiedName = type.getCanonicalName();
        if (name.endsWith("DtoDef")) {
            name = name.substring(0, name.length() - 3);
            qualifiedName = qualifiedName.substring(0, qualifiedName.length() - 3);
        }
    }

    protected Type(Class type, Class listType) {
        originalName = type.getSimpleName();
        name = type.getSimpleName();
        packageString = type.getPackage()
                            .getName();
        qualifiedName = type.getCanonicalName();
        if (name.endsWith("DtoDef")) {
            name = name.substring(0, name.length() - 3);
            qualifiedName = qualifiedName.substring(0, qualifiedName.length() - 3);
        }

        this.collectionType = new Type(listType);
    }

    public Type(TypeMirror typeMirror) {
        this.typeMirror = typeMirror;
        typeKind = typeMirror.getKind();

        if (typeKind == TypeKind.DECLARED) {
            DeclaredType declaredType = (DeclaredType) typeMirror;
            TypeElement typeElement = (TypeElement) processingEnv.getTypeUtils().asElement(typeMirror);
            originalName = typeElement.getSimpleName().toString();
            name = typeElement.getSimpleName().toString();
            packageString = typeElement.getEnclosingElement().toString();
            qualifiedName = typeElement.getQualifiedName().toString();

            QuickDto[] annotations = typeElement.getAnnotationsByType(QuickDto.class);
            if (annotations.length > 0) {
                isQuickDto = true;
                name = name.substring(0, name.length() - DefSuffix.length());
                qualifiedName = qualifiedName.substring(0, qualifiedName.length() - DefSuffix.length());
            }

            QuickCopy[] copyAnnotations = typeElement.getAnnotationsByType(QuickCopy.class);
            if (copyAnnotations.length > 0) {
                isQuickCopy = true;
            }

            List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
            if (typeArguments.size() > 0) {
                genericTypes = new ArrayList<>();
                for (TypeMirror genericMirror: typeArguments) {
                    genericTypes.add(new Type(genericMirror));
                }
                collectionType = new Type(typeArguments.get(0));
                if (this.collectionType.isQuickDto()) {
                    isQuickDtoList = true;
                }
            }
            if (isCollection()) {
                collectionType = genericTypes.get(0);
            }

        } else if (typeKind == TypeKind.ERROR && !processingType.name.equals(typeMirror.toString())) {
            throw new DelayParseException(typeMirror);

        } else {
            name = typeMirror.toString();
            packageString = "";
            qualifiedName = name;
        }
    }

    public String getConcreteCollection() {
        switch (qualifiedName) {
            case "java.util.Set":
                return "java.util.HashSet";
            case "java.util.Collection":
            case "java.util.List":
                return "java.util.ArrayList";
            default:
                return "UnknownCollection";
        }
    }

    public boolean isQuickDto() {
        return isQuickDto;
    }

    public boolean isQuickDtoList() {
        return isQuickDtoList;
    }

    public boolean isCollection() {
        return qualifiedName.equals("java.util.Collection") || qualifiedName.equals("java.util.Set") || qualifiedName.equals("java.util.List");
    }

    public boolean isQuickCopy() {
        return isQuickCopy;
    }

    public boolean isPrimitive() {
        return typeKind.isPrimitive();
    }

    public String getPrimitiveBoxType() {
        if (primitiveBoxType == null && isPrimitive()) {
            if ("int".equals(name)) {
                primitiveBoxType = "Integer";
            } else if ("boolean".equals(name)) {
                primitiveBoxType = "Boolean";
            } else if ("long".equals(name)) {
                primitiveBoxType = "Long";
            } else if ("double".equals(name)) {
                primitiveBoxType = "Double";
            } else if ("float".equals(name)) {
                primitiveBoxType = "Float";
            } else if ("char".equals(name)) {
                primitiveBoxType = "Character";
            } else if ("byte".equals(name)) {
                primitiveBoxType = "Byte";
            } else if ("short".equals(name)) {
                primitiveBoxType = "Short";
            }
        }
        return primitiveBoxType;
    }

    public String getDefaultPrimitiveValue() {
        if ("boolean".equals(name)) {
            return "false";
        } else {
            return "0";
        }
    }

    public String getGetAccessorPrefix() {
        if ("boolean".equals(name)) {
            return "is";
        } else {
            return "get";
        }
    }

    public boolean isImportable() {
        return !isPrimitive() && !"java.lang".equals(packageString) && !name.endsWith("[]");
    }

    public boolean isBoolean() {
        return "boolean".equals(name) || "Boolean".equals(name);
    }

    public Type getCollectionType() {
        return collectionType;
    }

    public ArrayList<Type> getGenericTypes() {
        return genericTypes;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getName() {
        return name;
    }

    public String getPackageString() {
        return packageString;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    public TypeMirror getTypeMirror() {
        return typeMirror;
    }

    public TypeKind getTypeKind() {
        return typeKind;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Type)) {
            return false;
        }
        Type type = (Type) o;
        return Objects.equals(packageString, type.packageString) && Objects.equals(name, type.name) &&
              Objects.equals(collectionType, type.collectionType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(packageString, name);
    }

    @Override
    public String toString() {
        if (collectionType != null) {
            return name + "<" + collectionType + ">";
        }
        return name;
    }
}

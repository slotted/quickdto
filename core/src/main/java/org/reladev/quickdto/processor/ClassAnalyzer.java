package org.reladev.quickdto.processor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleTypeVisitor7;
import javax.tools.Diagnostic.Kind;

import com.sun.source.tree.MethodTree;
import com.sun.source.util.Trees;
import org.reladev.quickdto.shared.CopyFromOnly;
import org.reladev.quickdto.shared.CopyToOnly;
import org.reladev.quickdto.shared.EqualsHashCode;
import org.reladev.quickdto.shared.QuickDto;
import org.reladev.quickdto.shared.StrictCopy;

public class ClassAnalyzer {
    private ProcessingEnvironment processingEnv;
    private Trees trees;

    public ClassAnalyzer(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    public DtoDef processDtoDef(Element defElement) {
        processingEnv.getMessager().printMessage(Kind.NOTE, "QuickDto: " + defElement.getSimpleName());
        DtoDef dtoDef = new DtoDef();
        PackageElement packageElement = (PackageElement) defElement.getEnclosingElement();
        dtoDef.packageString = packageElement.getQualifiedName().toString();
        dtoDef.name = defElement.getSimpleName().toString();
        dtoDef.name = dtoDef.name.substring(0, dtoDef.name.length() - 3);
        dtoDef.qualifiedName = dtoDef.packageString + "." + dtoDef.name;

        addClassAnnotations(defElement, dtoDef);
        addFieldMethods(defElement, dtoDef);
        addSources(defElement, dtoDef);

        return dtoDef;
    }

    private void addClassAnnotations(Element subelement, DtoDef dtoDef) {
        List<? extends AnnotationMirror> annotationMirrors = subelement.getAnnotationMirrors();
        for (AnnotationMirror am : annotationMirrors) {
            if (!isQuickDtoAnntoation(am)) {
                dtoDef.annotations.add(am.toString());
            }
        }
    }


    private boolean isQuickDtoAnntoation(AnnotationMirror an) {
        return an.toString().startsWith("@org.reladev.quickdto");
    }

    private void addFieldMethods(Element defElement, DtoDef dtoDef) {
        for (Element subelement : defElement.getEnclosedElements()) {
            if (subelement.getKind() != ElementKind.CONSTRUCTOR) {
                TypeMirror mirror = subelement.asType();
                Component component = mirror.accept(getType, subelement);
                if (component instanceof DtoField) {
                    addField(subelement, (DtoField) component, dtoDef);

                } else if (component instanceof Method) {
                    Method method = (Method) component;
                    dtoDef.methods.add(method);
                    if (method.converter) {
                        dtoDef.addConverter(method);
                    }
                }
            }
        }
    }

    private void addField(Element subelement, DtoField field, DtoDef dtoDef) {
        if (subelement.getAnnotation(EqualsHashCode.class) != null) {
            field.setEqualsHashCode();
        }
        CopyFromOnly copyFromOnly = subelement.getAnnotation(CopyFromOnly.class);
        if (copyFromOnly != null) {
            field.setCopyFrom();
            if (!copyFromOnly.setter()) {
                field.setExcludeSetter();
            }
        }
        CopyToOnly copyToOnly = subelement.getAnnotation(CopyToOnly.class);
        if (copyToOnly != null) {
            field.setCopyTo();
            if (!copyToOnly.getter()) {
                field.setExcludeGetter();
            }
        }
        StrictCopy strictCopy = subelement.getAnnotation(StrictCopy.class);
        if (strictCopy != null) {
            field.setStrictCopy(strictCopy.value());

        } else {
            field.setStrictCopy(dtoDef.strictCopy);
        }

        addAnnotations(subelement, field);

        DtoField existing = dtoDef.fields.get(field.getAccessorName());
        if (existing == null) {
            dtoDef.fields.put(field.getAccessorName(), field);
        } else {
            processingEnv.getMessager().printMessage(Kind.WARNING, "Skipping duplicated field definition:" + field.getAccessorName());
        }
    }

    private void addSources(Element element, DtoDef dtoDef) {
        final String annotationName = QuickDto.class.getName();
        element.getAnnotationMirrors();
        for (AnnotationMirror am : element.getAnnotationMirrors()) {
            AnnotationValue action;
            if (annotationName.equals(am.getAnnotationType().toString())) {
                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : am.getElementValues().entrySet()) {
                    if ("source".equals(entry.getKey().getSimpleName().toString())) {
                        action = entry.getValue();
                        List sources = (List) action.getValue();
                        for (Object source : sources) {
                            String className = annotationParamToClassName(source);
                            addSource(dtoDef, className);
                        }

                    } else if ("strictCopy".equals(entry.getKey().getSimpleName().toString())) {
                        action = entry.getValue();
                        dtoDef.strictCopy = (boolean) action.getValue();

                    } else if ("fieldAnnotationsOnGetter".equals(entry.getKey().getSimpleName().toString())) {
                        action = entry.getValue();
                        dtoDef.fieldAnnotationsOnGetter = (boolean) action.getValue();

                    } else if ("implement".equals(entry.getKey().getSimpleName().toString())) {
                        action = entry.getValue();
                        List implementList = (List) action.getValue();
                        for (Object implement : implementList) {
                            String className = annotationParamToClassName(implement);
                            dtoDef.implementList.add(className);
                        }

                    } else if ("extend".equals(entry.getKey().getSimpleName().toString())) {
                        action = entry.getValue();
                        Object extend = action.getValue();
                        String className = extend.toString();
                        if (!className.equals(Object.class.getCanonicalName())) {
                            dtoDef.extend = className;
                        }

                    } else if ("copyMethods".equals(entry.getKey().getSimpleName().toString())) {
                        action = entry.getValue();
                        boolean copyMethods = (boolean) action.getValue();
                        if (copyMethods) {
                            trees = Trees.instance(processingEnv);
                        }
                    }
                }
            }
        }
    }

    private void addSource(DtoDef dtoDef, String className) {
        Source sourceDef = new Source();
        dtoDef.sources.add(sourceDef);
        Elements elementUtils = processingEnv.getElementUtils();
        sourceDef.type = className;
        TypeElement sourceType = elementUtils.getTypeElement(className);
        while (sourceType != null) {
            for (Element sourceSubEl : sourceType.getEnclosedElements()) {
                if (sourceSubEl instanceof ExecutableElement) {
                    String name = sourceSubEl.getSimpleName().toString();
                    int numParams = ((ExecutableElement) sourceSubEl).getParameters().size();

                    if (name.startsWith("set") && numParams == 1) {
                        String accessorName = name.substring(3);
                        DtoField field = dtoDef.fields.get(accessorName);
                        if (field != null) {
                            VariableElement paramElement = ((ExecutableElement) sourceSubEl).getParameters().get(0);
                            String toType = paramElement.asType().toString();
                            String fromType = field.getTypeString();
                            mapAccessorConverter(dtoDef, field, className, sourceSubEl, toType, fromType, sourceDef.setters, accessorName);
                        }
                    }
                    if (name.startsWith("get") && numParams == 0) {
                        String accessorName = name.substring(3);
                        DtoField field = dtoDef.fields.get(accessorName);
                        if (field != null) {
                            String fromType = ((ExecutableElement) sourceSubEl).getReturnType().toString();
                            String toType = field.getTypeString();
                            mapAccessorConverter(dtoDef, field, className, sourceSubEl, toType, fromType, sourceDef.getters, accessorName);
                        }
                    }
                    if (name.startsWith("is") && numParams == 0) {
                        String accessorName = name.substring(2);
                        DtoField field = dtoDef.fields.get(accessorName);
                        if (field != null) {
                            String fromType = ((ExecutableElement) sourceSubEl).getReturnType().toString();
                            String toType = field.getTypeString();
                            mapAccessorConverter(dtoDef, field, className, sourceSubEl, toType, fromType, sourceDef.getters, accessorName);
                        }
                    }
                }
            }

            if (sourceType.getSuperclass() instanceof NoType) {
                sourceType = null;
            } else {
                sourceType = (TypeElement) ((DeclaredType) sourceType.getSuperclass()).asElement();
            }
        }
    }


    private boolean mapAccessorConverter(DtoDef dtoDef, DtoField field, String sourceClass, Element sourceSubEl, String toType, String fromType, HashMap<String, Method> accessorMap, String accessorName) {
        boolean map = false;
        Method converter = null;
        if (!fromType.equals(toType)) {
            converter = dtoDef.getConverter(toType, fromType);
            if (converter != null) {
                map = true;
            } else {
                processingEnv.getMessager().printMessage(Kind.WARNING, "Type Mismatch(" + toType + ":" + fromType + ") for " + sourceClass + "." + sourceSubEl);
            }
        } else {
            map = true;
        }

        if (map) {
            field.setSourceMapped();
            accessorMap.put(accessorName, converter);
        }

        return map;
    }

    private void addAnnotations(Element subelement, DtoField field) {
        List<? extends AnnotationMirror> annotationMirrors = subelement.getAnnotationMirrors();
        for (AnnotationMirror am : annotationMirrors) {
            if (!isQuickDtoAnntoation(am)) {
                if (subelement.getKind() == ElementKind.FIELD) {
                    field.addFieldAnnotation(am.toString());

                } else if (subelement.getKind() == ElementKind.METHOD) {
                    if (subelement.getSimpleName().toString().startsWith("get")) {
                        field.addGetterAnnotation(am.toString());

                    } else if (subelement.getSimpleName().toString().startsWith("set")) {
                        field.addSetterAnnotation(am.toString());
                    }
                }

            }
        }
    }

    private String annotationParamToClassName(Object source) {
        String className = source.toString();
        className = className.substring(0, className.length() - 6);
        return className;
    }

    private final TypeVisitor<Component, Element> getType = new SimpleTypeVisitor7<Component, Element>() {
        @Override
        public Component visitPrimitive(PrimitiveType t, Element element) {
            DtoField field = new DtoField();
            field.setType(t);
            field.setFieldName(element.toString());
            field.setPrimitive();
            return field;
        }

        @Override
        public Component visitArray(ArrayType t, Element element) {
            DtoField field = new DtoField();
            field.setType(t);
            field.setFieldName(element.toString());
            return field;
        }

        @Override
        public Component visitDeclared(DeclaredType t, Element element) {
            DtoField field = new DtoField();
            field.setType(t);
            field.setFieldName(element.toString());
            return field;
        }

        @Override
        public Component visitTypeVariable(TypeVariable t, Element element) {
            processingEnv.getMessager().printMessage(Kind.ERROR, "Couldn't process Type:" + element);
            return super.visitTypeVariable(t, element);
        }

        @Override
        public Component visitUnknown(TypeMirror t, Element element) {
            processingEnv.getMessager().printMessage(Kind.ERROR, "Couldn't process Unknown:" + element);
            return super.visitUnknown(t, element);
        }

        @Override
        public Component visitUnion(UnionType t, Element element) {
            processingEnv.getMessager().printMessage(Kind.ERROR, "Couldn't process Union:" + element);
            return super.visitUnion(t, element);
        }

        @Override
        protected Component defaultAction(TypeMirror e, Element element) {
            if (element.toString().endsWith("Dto")) {
                processingEnv.getMessager().printMessage(Kind.ERROR, "Couldn't process Dto:" + element + ".  Use DtoDef instead.");
            } else {
                processingEnv.getMessager().printMessage(Kind.ERROR, "Couldn't process Action:" + element);
            }
            return super.defaultAction(e, element);
        }

        @Override
        public Component visitNull(NullType t, Element element) {
            processingEnv.getMessager().printMessage(Kind.ERROR, "Couldn't process Null:" + element);
            return super.visitNull(t, element);
        }

        @Override
        public Component visitWildcard(WildcardType t, Element element) {
            processingEnv.getMessager().printMessage(Kind.ERROR, "Couldn't process Wildcard:" + element);
            return super.visitWildcard(t, element);
        }

        @Override
        public Component visitNoType(NoType t, Element element) {
            processingEnv.getMessager().printMessage(Kind.ERROR, "Couldn't process NoType:" + element);
            return super.visitNoType(t, element);
        }

        @Override
        public Component visitExecutable(ExecutableType t, Element element) {
            Component component = null;

            String name = element.toString();
            if ((name.startsWith("get") || name.startsWith("is")) && t.getReturnType().getKind() != TypeKind.VOID && t.getParameterTypes().size() == 0) {
                component = t.getReturnType().accept(getType, element);

            } else if (name.startsWith("set") && t.getReturnType().getKind() == TypeKind.VOID && t.getParameterTypes().size() == 1) {
                component = t.getParameterTypes().get(0).accept(getType, element);
            }

            if (component instanceof DtoField) {
                int start = 0;
                if (name.startsWith("set") || name.startsWith("get")) {
                    start = 3;
                } else if (name.startsWith("is")) {
                    start = 2;
                }
                int end = name.indexOf('(');
                if (end == -1) {
                    end = name.length();
                }
                ((DtoField) component).setFieldName(name.substring(start, end));

            } else if (element.toString().startsWith("convert(") && t.getParameterTypes().size() == 1) {
                Method method = new Method();
                method.converter = true;
                method.toType = t.getReturnType().toString();
                method.fromType = t.getParameterTypes().get(0).toString();

                if (trees != null) {
                    MethodScanner methodScanner = new MethodScanner();
                    MethodTree methodTree = methodScanner.scan((ExecutableElement) element, trees);
                    method.body = "\t" + methodTree.toString().replace("\n", "\n\t");
                    method.isStatic = element.getModifiers().contains(Modifier.STATIC);

                } else if (element.getModifiers().contains(Modifier.STATIC)) {
                    method.isStatic = true;

                } else {
                    processingEnv.getMessager().printMessage(Kind.WARNING, "IGNORING (" + element + ") it must be 'static' or copyMethod true");
                    method = null;
                }
                component = method;

            } else if (trees != null) {
                Method method = new Method();
                MethodScanner methodScanner = new MethodScanner();
                MethodTree methodTree = methodScanner.scan((ExecutableElement) element, trees);
                method.body = "\t" + methodTree.toString().replace("\n", "\n\t");

                component = method;

            } else {
                processingEnv.getMessager().printMessage(Kind.WARNING, "--IGNORING Method:" + element + " - If method should be copied, set \"copyMethod\" in @QuickDto");
            }

            return component;
        }

    };


}
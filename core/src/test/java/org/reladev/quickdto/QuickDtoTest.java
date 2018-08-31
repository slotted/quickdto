package org.reladev.quickdto;

import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import com.matthewtamlin.avatar.rules.AvatarRule;
import org.junit.Before;
import org.junit.Rule;
import org.reladev.quickdto.processor.QuickDtoProcessor;

public class QuickDtoTest {

    public String[] testClasses = {"src/test/java/org/reladev/quickdto/test_classes/BasicConvertDtoDef.java",
          "src/test/java/org/reladev/quickdto/test_classes/BasicConvertImpl.java",
          "src/test/java/org/reladev/quickdto/test_classes/BasicTypesDtoDef.java",
          "src/test/java/org/reladev/quickdto/test_classes/BasicTypesImpl.java",
          "src/test/java/org/reladev/quickdto/test_classes/TestClassDtoDef.java",
          "src/test/java/org/reladev/quickdto/test_classes/TestClassImpl.java",};

    @Rule
    public final AvatarRule rule = AvatarRule.builder().withSourcesAt(testClasses).build();

    public TypeElement elementBasicConvertDtoDef;
    public TypeElement elementBasicConvertImpl;
    public TypeElement elementBasicTypesDtoDef;
    public TypeElement elementBasicTypesImpl;
    public TypeElement elementTestClassDtoDef;
    public TypeElement elementTestClassImpl;

    @Before
    public void setupUtils() {
        QuickDtoProcessor.processingEnv = rule.getProcessingEnvironment();
        QuickDtoProcessor.messager = rule.getMessager();

        Set<Element> elements = rule.getRootElements();
        for (Element element : elements) {
            switch (element.toString()) {
                case "org.reladev.quickdto.test_classes.BasicConvertDtoDef":
                    elementBasicConvertDtoDef = (TypeElement) element;
                    break;
                case "org.reladev.quickdto.test_classes.BasicConvertImpl":
                    elementBasicConvertImpl = (TypeElement) element;
                    break;
                case "org.reladev.quickdto.test_classes.BasicTypesDtoDef":
                    elementBasicTypesDtoDef = (TypeElement) element;
                    break;
                case "org.reladev.quickdto.test_classes.BasicTypesImpl":
                    elementBasicTypesImpl = (TypeElement) element;
                    break;
                case "org.reladev.quickdto.test_classes.TestClassDtoDef":
                    elementTestClassDtoDef = (TypeElement) element;
                    break;
                case "org.reladev.quickdto.test_classes.TestClassImpl":
                    elementTestClassImpl = (TypeElement) element;
                    break;
            }
        }
    }

}
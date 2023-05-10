package com.oukq.cocktailhelper.toolwindow;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiJavaDocumentedElement;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocTag;

import java.util.Arrays;

/**
 * @tableName what_string_data
 * @comment whatDate
 */
public class PsiDocumentHelper {

    public static String findTag(PsiJavaDocumentedElement element, String tabName) {
        PsiDocComment docComment = element.getDocComment();
        if (docComment == null) {
            return null;
        }
        return Arrays.stream(docComment.getTags()).filter(t -> t.getName().equals(tabName)).findFirst()
                .map(PsiDocTag::getValueElement)
                .map(PsiElement::getText)
                .orElse(null);
    }

    public static void delTag(Project project, PsiJavaDocumentedElement element, String tagName) {
        WriteCommandAction.runWriteCommandAction(project, () -> {
            PsiDocComment docComment = element.getDocComment();
            if (docComment == null) {
                return;
            }
            PsiDocTag docTag = docComment.findTagByName(tagName);
            if (docTag == null) {
                return;
            }
            docTag.delete();
            if (docComment.getTags().length == 0) {
                docComment.delete();
            }
        });
    }

    public static void addOrReplaceTag(Project project, PsiJavaDocumentedElement element, String tagName,
                                       String content) {
        WriteCommandAction.runWriteCommandAction(project, () -> {
            PsiDocComment docComment = element.getDocComment();
            PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
            if (docComment == null) {
                docComment = factory.createDocCommentFromText("/**\n*/");
                element.addBefore(docComment, element.getFirstChild());
                docComment = element.getDocComment();
                addToDoc(factory, docComment, tagName, content);
            } else {
                PsiDocTag docTag = docComment.findTagByName(tagName);
                if (docTag == null) {
                    addToDoc(factory, docComment, tagName, content);
                } else {
                    PsiDocTag newTag = newDocTag(factory, tagName, content);
                    docComment.addBefore(newTag, docTag);
                    docTag.delete();

                }
            }
        });
    }

    private static void addToDoc(PsiElementFactory factory, PsiDocComment doc, String tabName,
                                 String content) {
        PsiDocTag tag = newDocTag(factory, tabName, content);
        doc.add(tag);
    }

    private static PsiDocTag newDocTag(PsiElementFactory factory, String tabName, String content) {
        return factory.createDocTagFromText(String.format("@%s %s", tabName, content));
    }
}

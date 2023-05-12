package com.oukq.cocktailhelper.toolwindow;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiJavaDocumentedElement;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocTag;

import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * @tableName what_string_data
 * @comment whatDate
 */
public class PsiDocumentHelper {

	private static final Pattern EMPTY_COMMENT = Pattern.compile("\\/\\*[\\*\\s]*\\*\\/");

	public static String findTag(PsiJavaDocumentedElement element, String tabName) {
		PsiDocComment docComment = element.getDocComment();
		if (docComment == null) {
			return null;
		}
		return Arrays.stream(docComment.getTags()).filter(t -> t.getName().equals(tabName)).findFirst()
				.map(PsiDocTag::getText)
				.map(s -> s.substring(tabName.length() + 1).trim())// 上面得到 @tagName bla bla bla 的整个字符串, 需要截掉  @tabName
				.map(s -> !s.contains("\n") ? s : s.substring(0, s.indexOf("\n"))) //截掉回车之后的, 算下一行了
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
			if (isEmptyDoc(docComment)) {
				docComment.delete();
			}
		});
	}

	private static boolean isEmptyDoc(PsiDocComment docComment) {
		if (docComment.getTags().length != 0) {
			return false;
		}
		return EMPTY_COMMENT.matcher(docComment.getText()).matches();
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

package com.oukq.cocktailhelper.toolwindow;

import com.google.common.collect.Sets;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiEnumConstant;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.components.JBTextField;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class InsertKeysPanel extends JPanel {
	private static final long serialVersionUID = 9045956296906656792L;

	private Project project;
	private JBTextField descTf = new JBTextField();
	private JBTextField fieldNameTf = new JBTextField();
	private JBTextField decReasonIdTf = new JBTextField();
	private JBTextField getApproachIdTf = new JBTextField();
	private JBTextField activityTypeTf = new JBTextField();
	private JBTextField mailTemplateIdsTf = new JBTextField();
	private JBTextField redPointType = new JBTextField();
	private JBTextField vipPrivilegeType = new JBTextField();
	private JButton confirmBtn = new JButton("插入");

	private Map<Object, InsertKeyParam> object2InsertParam = new HashMap<>();

	public InsertKeysPanel(Project project) {
		super();
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		this.project = project;

		addPair("field名", fieldNameTf, null);
		addPair("key描述", descTf, null);
		addPair("GetApproach(id)", getApproachIdTf, new InsertKeyParam("com.bt.pjaqm.socket.ext.material.GetApproach",
				Sets.newHashSet(PsiModifier.PUBLIC, PsiModifier.STATIC, PsiModifier.FINAL),
				"new GetApproach((short) #content#, true, \"#fieldDesc#\")", () -> getApproachIdTf.getText().trim(),
				false));
		addPair("DecReason(id)", decReasonIdTf, new InsertKeyParam("com.bt.pjaqm.socket.ext.material.DecReason",
				Sets.newHashSet(PsiModifier.PUBLIC, PsiModifier.STATIC, PsiModifier.FINAL),
				"new DecReason((short) #content#, \"#fieldDesc#\")", () -> decReasonIdTf.getText().trim(),
				false));
		addPair("ActivityType(填了就插)", activityTypeTf,
				new InsertKeyParam("com.bt.pjaqm.common.ext.activity.define.EActivityType",
						null,
						"ActivityType.#fieldName#, \"#fieldDesc#\", true, EProcessType.SPECIAL_ACTIVITY",
						() -> activityTypeTf.getText().trim(),
						true));
		addPair("MailTemplateIds(id)", mailTemplateIdsTf,
				new InsertKeyParam("com.bt.pjaqm.common.ext.mail.MailTemplateIds",
						null,
						"#content#",
						() -> mailTemplateIdsTf.getText().trim(),
						true).setFieldType("int")
						.setLineComment("#fieldDesc#")
		);
		addPair("RedPointType(id)", redPointType, new InsertKeyParam("com.bt.pjaqm.socket.ext.redpoint.RedPointType",
				Sets.newHashSet(PsiModifier.PUBLIC, PsiModifier.STATIC, PsiModifier.FINAL),
				"new RedPointType(#content#, \"#fieldDesc#\")", () -> redPointType.getText().trim(),
				true));

		addPair("VipPrivilegeType(id)", vipPrivilegeType,
				new InsertKeyParam("com.bt.pjaqm.socket.ext.pay.sub.vip.VipPrivilegeType",
						null,
						"#content#, \"#fieldDesc#\"", () -> vipPrivilegeType.getText().trim(),
						true));
		add(confirmBtn);
		confirmBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				insertAllKeys();
			}
		});
	}

	private void addPair(String tfName, JTextField tf, InsertKeyParam param) {
		CocketailHelper.addLabelAndTf(tfName, tf, this);
		if (param != null) {
			object2InsertParam.put(tf, param);
		}
	}

	private void insertAllKeys() {
		if (StringUtils.isBlank(fieldNameTf.getText()) || StringUtils.isBlank(descTf.getText())) {
			Notifications.Bus.notify(
					new Notification("External annotations",
							"field名 或 key描述 未填",
							NotificationType.INFORMATION),
					project);
			return;
		}
		for (com.oukq.cocktailhelper.toolwindow.InsertKeysPanel.InsertKeyParam param : object2InsertParam.values()) {
			if (!StringUtils.isBlank(param.getContentParamProvider().get())) {
				insertKey(param);
			}
		}
	}

	private void insertKey(InsertKeyParam param) {
		PsiClass cls = getAndOpenFile(param.getClassName());
		if (cls == null) {
			LoggerFactory.getLogger(InsertKeysPanel.class).error("not found file:[{}]", param.getClassName());
			return;
		}
		String content = formatContent(param.getContentFormatter(), param);
		if (cls.isEnum()) {
			insertConstantIntoEnum(cls, param.getFieldName(), content, param.isAddToLast());
		} else {
			PsiClassType clsType = PsiType.getTypeByName(
					param.getFieldType() != null ? param.getFieldType() : param.getClassName(), project,
					cls.getResolveScope());
			insertFieldIntoCls(cls, param.getFieldName(),
					content,
					param.getModifiers(), clsType, param.isAddToLast(),
					param.getLineComment() == null ? null : formatContent("//" + param.getLineComment(), param));
		}

	}

	/**
	 * 格式化内容
	 */
	private String formatContent(String formatter, InsertKeyParam param) {
		return formatter.replace("#fieldName#", param.getFieldName())
				.replace("#fieldDesc#", param.getFieldDesc())
				.replace("#content#", param.getContentParamProvider().get())
				;
	}

	private void insertFieldIntoCls(PsiClass psiClass, String fieldName, String content, Set<String> anchorModifiers,
			PsiType fieldType, boolean addToLast, String lineComment) {
		List<PsiField> fields = Arrays.stream(psiClass.getFields())
				.filter(s -> hasAllModifiers(s.getModifierList(), anchorModifiers)).filter(
						s -> s.getType().getCanonicalText().equals(fieldType.getCanonicalText()))
				.collect(Collectors.toList());
		PsiElementFactory factory = PsiElementFactory.getInstance(project);
		PsiField field = factory.createField(fieldName, fieldType);
		PsiExpression expression = factory
				.createExpressionFromText(content, null);
		field.setInitializer(expression);
		if (anchorModifiers != null && !anchorModifiers.isEmpty()) {
			for (String modifier : anchorModifiers) {
				field.getModifierList().setModifierProperty(modifier, true);
			}
		} else {
			field.getModifierList().setModifierProperty(PsiModifier.PRIVATE, false);
		}
		PsiComment comment = null;

		if (StringUtils.isNotBlank(lineComment)) {
			comment = factory.createCommentFromText(lineComment, null);
		}
		addRelativeToAnchors(psiClass, field, fields, addToLast, comment);
	}

	/**
	 * 插入枚举
	 */
	private void insertConstantIntoEnum(PsiClass psiClass, String fieldName, String content, boolean addToLast) {
		List<PsiField> fields = Arrays.stream(psiClass.getFields())
				.filter(s -> s instanceof PsiEnumConstant).collect(Collectors.toList());
		PsiEnumConstant constant = PsiElementFactory.getInstance(project)
				.createEnumConstantFromText(String.format("%s(%s)", fieldName, content), null);
		addRelativeToAnchors(psiClass, constant, fields, addToLast, null);
	}

	/**
	 * 参照物列表中添加到最开始或者最后
	 */
	private void addRelativeToAnchors(PsiClass psiClass, PsiElement element, List<? extends PsiElement> anchors,
			boolean addToLast, PsiComment comment) {
		WriteCommandAction.runWriteCommandAction(project, () -> {
			PsiElement addReturn;
			if (anchors.isEmpty()) {
				PsiField firstAnyField = Arrays.stream(psiClass.getFields()).findFirst().orElse(null);
				if (firstAnyField != null) {
					addReturn = psiClass.addBefore(element, firstAnyField);
				} else {
					addReturn = psiClass.add(element);
				}
			} else {
				if (addToLast) {
					addReturn = psiClass.addAfter(element, anchors.get(anchors.size() - 1));
				} else {
					addReturn = psiClass.addBefore(element, anchors.get(0));
				}
			}
			if (comment != null) {
				psiClass.addAfter(comment, addReturn);
			}
		});
	}

	private PsiClass getAndOpenFile(String classFullPath) {
		PsiClass psiClass = JavaPsiFacade.getInstance(project)
				.findClass(classFullPath, GlobalSearchScope.allScope(project));
		if (psiClass == null) {
			return null;
		}
		FileEditorManager.getInstance(project).openFile(psiClass.getContainingFile().getVirtualFile(), true);
		return psiClass;
	}

	private boolean hasAllModifiers(PsiModifierList modifierList, Set<String> anchorModifiers) {
		if (anchorModifiers == null || anchorModifiers.isEmpty()) {
			return true;
		}
		for (String modifier : anchorModifiers) {
			if (!modifierList.hasModifierProperty(modifier)) {
				return false;
			}
		}
		return true;
	}

	private class InsertKeyParam {

		private String className;

		private String fieldType;
		private Set<String> modifiers;
		private String contentFormatter;
		private Supplier<String> contentParamProvider;
		private boolean addToLast;

		private String lineComment;

		public InsertKeyParam(String className, Set<String> modifiers,
				String contentFormatter,
				Supplier<String> contentParamProvider,
				boolean addToLast) {
			this.className = className;
			this.modifiers = modifiers;
			this.contentFormatter = contentFormatter;
			this.contentParamProvider = contentParamProvider;
			this.addToLast = addToLast;
		}

		public String getClassName() {
			return className;
		}

		public String getFieldName() {
			return fieldNameTf.getText();
		}

		public String getFieldDesc() {
			return descTf.getText();
		}

		public Set<String> getModifiers() {
			return modifiers == null ? Collections.emptySet() : modifiers;
		}

		public String getContentFormatter() {
			return contentFormatter;
		}

		public Supplier<String> getContentParamProvider() {
			return contentParamProvider;
		}

		public boolean isAddToLast() {
			return addToLast;
		}

		public InsertKeyParam setFieldType(String fieldType) {
			this.fieldType = fieldType;
			return this;
		}

		public String getFieldType() {
			return fieldType;
		}

		public InsertKeyParam setLineComment(String lineComment) {
			this.lineComment = lineComment;
			return this;
		}

		public String getLineComment() {
			return lineComment;
		}
	}
}

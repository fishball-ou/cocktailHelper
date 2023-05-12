package com.oukq.cocktailhelper.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaDocumentedElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiModifier;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.table.JBTable;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CreateTableSqlPanel extends JPanel {

	private static final String[] HEADERS = { "主键", "名字", "类型", "注释" };
	private static final long serialVersionUID = -6110700278480726809L;
	private final DocumentAdapter tableNameTfListener;
	private final DocumentAdapter commentTfListener;
	private JBTextField tableNameTf = new JBTextField();
	private JBTextField tableNameComment = new JBTextField();

	/**
	 * @dataType varchar(20)
	 * @primary true
	 */
	private JBTextArea outputTA = new JBTextArea();
	private final JBTable columnsTable;
	private Project project;

	private MyDataModel columnsDataModel;

	/**
	 * Creates a new <code>JPanel</code> with a double buffer and a flow layout.
	 */
	public CreateTableSqlPanel(Project project) {
		super(new GridBagLayout());
		this.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		this.project = project;
		columnsTable = createFieldTable();
		//		tableNameTf.setPreferredSize(new Dimension(tableNameTf.getPreferredSize().width, 5));
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.weighty = 1;
		constraints.weightx = 0.5;
		//		constraints.fill = GridBagConstraints.HORIZONTAL;
		this.add(tableNameTf, constraints);
		tableNameTfListener = new DocumentAdapter() {
			@Override
			protected void textChanged(@NotNull DocumentEvent e) {
				if (columnsDataModel != null && columnsDataModel.getCls() != null) {
					modifyPsiDocument(columnsDataModel.getCls(), "tableName", tableNameTf.getText());
					refreshSql();
				}
			}
		};
		tableNameTf.getDocument().addDocumentListener(tableNameTfListener);

		constraints.gridx = 1;
		constraints.weightx = 0.5;
		this.add(tableNameComment, constraints);
		commentTfListener = new DocumentAdapter() {
			@Override
			protected void textChanged(@NotNull DocumentEvent e) {
				if (columnsDataModel != null && columnsDataModel.getCls() != null) {
					modifyPsiDocument(columnsDataModel.getCls(), "comment", tableNameComment.getText());
					refreshSql();
				}
			}
		};
		tableNameComment.getDocument().addDocumentListener(commentTfListener);
		//下面修正为一行一个元素
		constraints.gridx = 0;
		constraints.weightx = 1;
		constraints.gridwidth = 2;

		constraints.gridy = 1;
		constraints.weighty = 4;
		constraints.fill = GridBagConstraints.BOTH;
		//		columnConst.fill = GridBagConstraints.HORIZONTAL;
		this.add(new JBScrollPane(columnsTable), constraints);

		outputTA.setLineWrap(true); // Enable text wrapping
		outputTA.setWrapStyleWord(true); // Enable word wrapping
		JScrollPane scrollPane = new JBScrollPane(outputTA);
		constraints.gridy = 2;
		constraints.weighty = 7;
		//		columnConst.fill = GridBagConstraints.HORIZONTAL;
		this.add(scrollPane, constraints);
	}

	private JBTable createFieldTable() {
		columnsDataModel = new MyDataModel();
		return new JBTable(columnsDataModel);
	}

	public void refresh(VirtualFile file) {
		clearAndSet("");
		try {
			PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
			if (psiFile == null) {
				clearAndSet(String.format("找不到 %s 的 psi结构", file.getName()));
				return;
			}
			if (!psiFile.getLanguage().getDisplayName().equals("Java")) {
				clearAndSet(String.format(" %s 非java文件", file.getName()));
				return;
			}
			PsiElement[] elements = psiFile.getChildren();
			for (PsiElement element : elements) {
				if (element instanceof PsiClass) {
					if (findDataCls(((PsiClass) element))) {
						break;
					}
				}
			}
		} catch (Exception e) {
			clearAndSet(e.getMessage());
		}
	}

	private boolean findDataCls(PsiClass cls) {
		if (cls.isRecord() || cls.isEnum() || cls.isInterface() || cls.getModifierList() == null || cls.getModifierList()
				.hasModifierProperty(
						PsiModifier.ABSTRACT)) {
			return false;
		}
		fillTableInfo(cls);
		PsiField[] fields = cls.getFields();
		List<MyField> columnFields = new ArrayList<>();
		int index = 0;
		int primaryKeyNum = predicatePrimaryNum(cls);

		boolean hasPrimaryComment = false;
		for (PsiField field : fields) {
			if (field.getModifierList() == null || field.getModifierList()
					.hasModifierProperty(PsiModifier.STATIC) || field.getModifierList()
					.hasModifierProperty(PsiModifier.TRANSIENT)) {
				continue;
			}
			if (!hasPrimaryComment) {
				hasPrimaryComment = findTag(field, "primary") != null;
			}
			columnFields.add(createColumnField(!hasPrimaryComment && index < primaryKeyNum, field));
			index++;
		}
		columnsDataModel.setFields(cls, columnFields);
		refreshSql();
		return true;
	}

	private void refreshSql() {
		if (columnsDataModel != null) {
			clearAndSet(CreateTableSqlHelper.format(tableNameTf.getText(), tableNameComment.getText(),
					columnsDataModel.getFields()));
		}
	}

	private void fillTableInfo(PsiClass cls) {
		String clsComment = findTag(cls, "comment");
		String tableName = findTag(cls, "tableName");
		if (StringUtils.isBlank(tableName)) {
			tableName = transformName(cls.getName());
		}

		tableNameTf.getDocument().removeDocumentListener(tableNameTfListener);
		tableNameComment.getDocument().removeDocumentListener(commentTfListener);
		tableNameComment.setText(clsComment);
		tableNameTf.setText(tableName);
		tableNameTf.getDocument().addDocumentListener(tableNameTfListener);
		tableNameComment.getDocument().addDocumentListener(commentTfListener);
	}

	private void notifyPrimaryUpdate() {
		List<MyField> columnFields = columnsDataModel.getFields();
		for (MyField field : columnFields) {
			modifyPsiDocument(field.getPsiField(), "primary", field.isPrimary() ? "true" : "");
		}
	}

	private int predicatePrimaryNum(PsiClass cls) {
		PsiClass mapElementCls = JavaPsiFacade.getInstance(project)
				.findClass("com.bt.game.common.cache.MapCacheElement", cls.getResolveScope());
		if (mapElementCls == null) {
			return 1;
		}
		if (cls.isInheritor(mapElementCls, true)) {
			return 2;
		}
		return 1;
	}

	private String transformName(String name) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if (Character.isUpperCase(c)) {
				if (sb.length() > 0) {
					sb.append("_");
				}
				c = Character.toLowerCase(c);
			}
			sb.append(c);
		}
		return sb.toString();
	}

	private MyField createColumnField(boolean predicatePrimary, PsiField field) {
		String fieldName = field.getName();
		String dataTypeInComment = Optional.ofNullable(findTag(field, "dataType")).orElse("");
		Boolean primary = Optional.ofNullable(findTag(field, "primary")).filter(StringUtils::isNotBlank)
				.map("true"::equalsIgnoreCase).orElse(null);

		String dataType;
		boolean defaultDataType = true;
		if (StringUtils.isBlank(dataTypeInComment)) {
			dataType = field.getType().getCanonicalText();
			Integer length = Optional.ofNullable(findTag(field, "length")).filter(StringUtils::isNumeric)
					.map(Integer::parseInt).orElse(50);
			dataType = parseDataType(dataType, length);
		} else {
			dataType = dataTypeInComment;
			defaultDataType = false;
		}
		String comment = Optional.ofNullable(findTag(field, "comment")).orElse("");

		return new MyField(field, primary != null ? primary : predicatePrimary, fieldName, dataType,
				defaultDataType, comment);
	}

	private String findTag(PsiJavaDocumentedElement field, String tabName) {
		return PsiDocumentHelper.findTag(field, tabName);
	}

	private String parseDataType(String javaDataType, int length) {
		switch (javaDataType) {
		case "int":
		case "java.lang.Integer":
		case "short":
		case "java.lang.Short":
			return "int(10)";
		case "long":
		case "java.lang.Long":
			return "bigint(20)";
		case "boolean":
		case "java.lang.Boolean":
			return "tinyint(1)";
		case "byte[]":
			return "bit(64)";
		case "java.lang.String":
			return String.format("varchar(%s)", length);
		case "java.time.LocalDateTime":
		case "java.time.LocalDate":
		case "java.util.Date":
			return "datetime(3)";

		default:
			return javaDataType;
		}
	}

	private void clearAndSet(String message) {
		outputTA.setText("" + message);
	}

	private void append(String message) {
		outputTA.append(message);
	}

	private class MyDataModel extends AbstractTableModel {

		private PsiClass cls;
		private List<MyField> fields;

		public List<MyField> getFields() {
			return fields;
		}

		public void setFields(PsiClass cls, List<MyField> fields) {
			this.cls = cls;
			this.fields = fields;
			fireTableDataChanged();
		}

		public PsiClass getCls() {
			return cls;
		}

		public MyDataModel() {
			fireTableStructureChanged();
		}

		private static final long serialVersionUID = 488485029628100589L;

		@Override
		public int getRowCount() {
			return fields == null ? 0 : fields.size();
		}

		@Override
		public int getColumnCount() {
			return HEADERS.length;
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			if (columnIndex == 0) {
				return Boolean.class;
			} else {
				return String.class;
			}
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			return fields == null ? null : fields.get(rowIndex).getValue(columnIndex);
		}

		@Override
		public String getColumnName(int column) {
			return HEADERS[column];
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			if (fields != null) {
				fields.get(rowIndex).setValue(columnIndex, aValue);
			}
			clearAndSet(CreateTableSqlHelper.format(tableNameTf.getText(), "", this.fields));
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return columnIndex != 1;
		}
	}

	private void modifyPsiDocument(PsiJavaDocumentedElement psiField, String tagName, String content) {
		if (StringUtils.isEmpty(content)) {
			PsiDocumentHelper.delTag(project, psiField, tagName);
		} else {
			PsiDocumentHelper.addOrReplaceTag(project, psiField, tagName, content);
		}
	}

	public class MyField {
		private PsiField psiField;
		private boolean primary;
		private String fieldName;

		private boolean defaultDataType;
		private String dataType;
		private String comment;

		public MyField(PsiField psiField, boolean primary, String fieldName, String dataType,
				boolean defaultDataType,
				String comment) {
			this.psiField = psiField;
			this.primary = primary;
			this.fieldName = fieldName;
			this.dataType = dataType;
			this.defaultDataType = defaultDataType;
			this.comment = comment;
		}

		private Object getValue(int index) {
			switch (index) {
			case 0:
				return primary;
			case 1:
				return fieldName;
			case 2:
				return dataType;
			case 3:
				return comment;
			default:
				return null;
			}
		}

		private void setValue(int index, Object value) {
			switch (index) {
			case 0:
				primary = ((Boolean) value);
				//只有修改了某一个字段, 全部field刷新
				notifyPrimaryUpdate();
				break;
			case 1:
				fieldName = String.valueOf(value);
				break;
			case 2:
				dataType = String.valueOf(value);
				if (defaultDataType) {
					dataType = String.format("%s %s", dataType, "NOT NULL");
					defaultDataType = false;
				}
				modifyPsiDocument(getPsiField(), "dataType", dataType);
				break;
			case 3:
				comment = String.valueOf(value);
				modifyPsiDocument(getPsiField(), "comment", comment);
				break;
			default:
				break;
			}
		}

		public PsiField getPsiField() {
			return psiField;
		}

		public boolean isPrimary() {
			return primary;
		}

		public boolean isDefaultDataType() {
			return defaultDataType;
		}

		public String getFieldName() {
			return fieldName;
		}

		public String getDataType() {
			return dataType;
		}

		public String getComment() {
			return comment;
		}
	}

}

package com.oukq.cocktailhelper.toolwindow;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

public class CreateTableSqlHelper {
	private static final String formatter = "CREATE TABLE `#tableName#` (\n" +
			"#columnsContent#" +
			") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='#tableComment#';\n";

	private static final String COL_DEFAULT_FORMATTER = "`#column#` #dataType# NOT NULL";
	private static final String COL_REPLACE_FORMATTER = "`#column#` #dataType#";

	private static final String PRIMARY_FORMATTER = "PRIMARY KEY (#primaryKeys#)";

	private static final String COMMENT_FORMATTER = " COMMENT '#comment#' ";

	public static String format(String tableName, String tableComment, List<CreateTableSqlPanel.MyField> fields) {
		String columnsContent = buildColumnsContent(fields);
		return formatter.replace("#tableName#", tableName)
				.replace("#tableComment#", tableComment)
				.replace("#columnsContent#", columnsContent);
	}

	private static String buildColumnsContent(List<CreateTableSqlPanel.MyField> fields) {
		StringBuilder columnsContent = new StringBuilder();
		boolean hasPrimary = fields.stream().anyMatch(f -> f.isPrimary());
		for (int i = 0; i < fields.size(); i++) {
			CreateTableSqlPanel.MyField field = fields.get(i);
			columnsContent.append("  ");
			columnsContent.append(buildSingleColumn(field));
			if (hasPrimary || i != fields.size() - 1) {
				columnsContent.append(",");
			}
			columnsContent.append("\n");
		}
		if (hasPrimary) {
			columnsContent.append("  ");
			columnsContent.append(PRIMARY_FORMATTER.replace("#primaryKeys#", StringUtils.join(fields.stream().filter(
					CreateTableSqlPanel.MyField::isPrimary).map(f -> String.format("'%s'", f.getFieldName())).collect(
					Collectors.toList()), ","))).append("\n");
		}
		return columnsContent.toString();
	}

	private static String buildSingleColumn(CreateTableSqlPanel.MyField field) {
		String formatter = field.isDefaultDataType() ? COL_DEFAULT_FORMATTER : COL_REPLACE_FORMATTER;
		String part = formatter.replace("#column#", field.getFieldName()).replace("#dataType#", field.getDataType());
		if (!StringUtils.isEmpty(field.getComment())) {
			part += COMMENT_FORMATTER.replace("#comment#", field.getComment());
		}
		return part;
	}
}

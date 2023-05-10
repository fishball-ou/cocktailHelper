package com.oukq.cocktailhelper.toolwindow;

import com.intellij.application.options.CodeStyleSchemesConfigurable;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.options.SchemeFactory;
import com.intellij.openapi.options.SchemeImportException;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.psi.codeStyle.CodeStyleScheme;
import com.intellij.psi.codeStyle.CodeStyleSchemes;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.psi.impl.source.codeStyle.CodeStyleSchemeImpl;
import com.intellij.psi.impl.source.codeStyle.CodeStyleSettingsLoader;
import com.oukq.cocktailhelper.inspection.InspectionBundle;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.InputStream;

public class CodeStyleImporter extends JPanel {

	private static final String DEFAULT_NAME = "bt_java_profile";
	private static final long serialVersionUID = 421153217911526411L;

	private Project project;

	/**
	 * Creates a new <code>JPanel</code> with a double buffer and a flow layout.
	 */
	public CodeStyleImporter(Project project) {
		this.project = project;
		JButton button = new JButton("导入bt_java_profile");
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				importCodeStyle();
			}
		});
		button.setBounds(5, 5, 30, 10);
		add(button);

	}

	private void importCodeStyle() {
		try {

			CodeStyleScheme existsScheme = CodeStyleSchemes.getInstance().getAllSchemes().stream()
					.filter(s -> s.getName().equals(DEFAULT_NAME)).findFirst().orElse(null);
			if (existsScheme != null) {
				Notifications.Bus.notify(
						new Notification("External annotations",
								InspectionBundle.message("codestyle.import.already.exists"),
								NotificationType.INFORMATION),
						project);
				return;
			}
			createScheme();
			Notifications.Bus.notify(
					new Notification("External annotations", InspectionBundle.message("codestyle.import.success"),
							NotificationType.INFORMATION),
					project);
			ShowSettingsUtil.getInstance()
					.showSettingsDialog(project, CodeStyleSchemesConfigurable.class);
		} catch (Exception e) {
			LoggerFactory.getLogger(CodeStyleImporter.class).error("ex in importCodeStyle", e);
		}
	}

	private CodeStyleScheme createScheme() throws Exception {
		InputStream fileStream = getClass().getClassLoader().getResourceAsStream("META-INF/bt_java_profile.xml");
		Element element = JDOMUtil.load(fileStream);
		final SchemeCreator schemeCreator = new SchemeCreator();
		CodeStyleScheme scheme = schemeCreator.createNewScheme("bt_java_profile");
		readSchemeFromDom(element, scheme);
		return scheme;
	}

	private static void readSchemeFromDom(@NotNull Element rootElement, @NotNull CodeStyleScheme scheme)
			throws SchemeImportException {
		CodeStyleSettings newSettings = CodeStyleSettingsManager.getInstance().createSettings();
		MyCodeStyleSettingLoader.myLoadSettings(rootElement, newSettings);
		newSettings.resetDeprecatedFields(); // Clean up if imported from legacy settings
		((CodeStyleSchemeImpl) scheme).setCodeStyleSettings(newSettings);
	}

	private static class MyCodeStyleSettingLoader extends CodeStyleSettingsLoader {
		public static void myLoadSettings(@NotNull Element rootElement, @NotNull CodeStyleSettings settings)
				throws SchemeImportException {
			loadSettings(rootElement, settings);
		}
	}

	private class SchemeCreator implements SchemeFactory<CodeStyleScheme> {

		@NotNull
		@Override
		public CodeStyleScheme createNewScheme(@Nullable String targetName) {
			if (targetName == null)
				targetName = ApplicationBundle.message("code.style.scheme.import.unnamed");
			CodeStyleScheme newScheme = CodeStyleSchemes.getInstance().createNewScheme(targetName, getCurrentScheme());
			CodeStyleSchemes.getInstance().addScheme(newScheme);
			return newScheme;
		}
	}

	private CodeStyleScheme getCurrentScheme() {
		return CodeStyleSchemes.getInstance().getCurrentScheme();
	}

}

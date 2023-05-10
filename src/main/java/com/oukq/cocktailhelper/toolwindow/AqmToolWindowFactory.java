// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.oukq.cocktailhelper.toolwindow;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

public class AqmToolWindowFactory implements ToolWindowFactory, DumbAware {

	private AqmToolWindowContent instance;

	@Override
	public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
		if (instance == null) {
			AqmToolWindowContent toolWindowContent = new AqmToolWindowContent(toolWindow);
			Content content = ContentFactory.getInstance()
					.createContent(toolWindowContent.getContentPanel(), "", false);
			toolWindow.getContentManager().addContent(content);
			instance = toolWindowContent;
		}
	}

	private static class AqmToolWindowContent implements Disposable {

		private final JBTabbedPane contentPanel = new JBTabbedPane();
		private final CreateTableSqlPanel createTableSqlPanel;

		public AqmToolWindowContent(ToolWindow toolWindow) {
			createTableSqlPanel = new CreateTableSqlPanel(toolWindow.getProject());
			InsertKeysPanel keysPanel = new InsertKeysPanel(toolWindow.getProject());
			contentPanel.addTab("创表生成器", createTableSqlPanel);
			contentPanel.addTab("强势插入", keysPanel);
			contentPanel.addTab("代码风格", new CodeStyleImporter(toolWindow.getProject()));
			toolWindow.getProject().getMessageBus().connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER,
					new FileEditorManagerListener() {

						@Override
						public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
							createTableSqlPanel.refresh(file);
						}

						@Override
						public void selectionChanged(@NotNull FileEditorManagerEvent event) {
							createTableSqlPanel.refresh(event.getNewFile());
						}

					});
			toolWindow.getProject().getMessageBus().connect().subscribe(VirtualFileManager.VFS_CHANGES,
					new BulkFileListener() {
						@Override
						public void after(@NotNull List<? extends @NotNull VFileEvent> events) {
							for (VFileEvent event : events) {
								if (!(event instanceof VFileContentChangeEvent)) {
									continue;
								}
								if (!event.isFromSave()) {
									continue;
								}
								createTableSqlPanel.refresh(event.getFile());
							}
						}
					});
		}

		public JTabbedPane getContentPanel() {
			return contentPanel;
		}

		/**
		 * Usually not invoked directly, see class javadoc.
		 */
		@Override
		public void dispose() {

		}
	}
}

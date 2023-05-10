package com.oukq.cocktailhelper.toolwindow;

import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;

import javax.swing.*;
import java.awt.*;

public class CocketailHelper {

	public static void addLabelAndTf(String tfName, JTextField tf, JComponent parent) {
		JBLabel label = new JBLabel(tfName);
		label.setMaximumSize(new Dimension(150, 30));
		label.setHorizontalAlignment(JLabel.RIGHT);
		tf.setMaximumSize(new Dimension(150, 30));
		JPanel container = new JBPanel<>();
		container.setLayout(new BoxLayout(container, BoxLayout.X_AXIS));
		container.add(label);
		container.add(Box.createHorizontalStrut(5));
		container.add(tf);
		container.setMaximumSize(new Dimension(300, 30));
		parent.add(container);
	}
}

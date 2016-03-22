package com.ninchat.swingclient;

import com.ninchat.client.model.IdentitySessionCreationMethod;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class CredentialsDialog extends JDialog {
	private JPanel contentPane;
	private JButton buttonOK;
	private JButton buttonCancel;
	private JTextField emailField;
	private JPasswordField passwordField;

	private SwingClient swingClient;

	public CredentialsDialog(SwingClient swingClient) {
		super(swingClient);
		this.swingClient = swingClient;

		setupUi();
		setContentPane(contentPane);
		setModal(true);
		getRootPane().setDefaultButton(buttonOK);

		buttonOK.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onOK();
			}
		});

		buttonCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onCancel();
			}
		});

		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				onCancel();
			}
		});

		contentPane.registerKeyboardAction(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onCancel();
			}
		}, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
	}

	private void onOK() {
		IdentitySessionCreationMethod method = new IdentitySessionCreationMethod();
		method.setType("email");
		method.setName(emailField.getText());
		method.setAuth(passwordField.getText());

		swingClient.start(method);

		dispose();
	}

	private void onCancel() {
		System.exit(0);
	}

	private void setupUi() {
		contentPane = new JPanel();
		contentPane.setLayout(new GridLayout(2, 1, 10, 10));
		final JPanel inputPanel = new JPanel();
		contentPane.add(inputPanel);

		inputPanel.setLayout(new GridLayout(2, 2, 10, 10));

		inputPanel.add(new JLabel("Email"));
		emailField = new JTextField();
		inputPanel.add(emailField);

		inputPanel.add(new JLabel("Password"));
		passwordField = new JPasswordField();
		inputPanel.add(passwordField);

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridLayout(1, 2, 10, 10));
		contentPane.add(buttonPanel);

		buttonOK = new JButton();
		buttonOK.setText("OK");
		buttonPanel.add(buttonOK);

		buttonCancel = new JButton();
		buttonCancel.setText("Cancel");
		buttonPanel.add(buttonCancel);
	}

}

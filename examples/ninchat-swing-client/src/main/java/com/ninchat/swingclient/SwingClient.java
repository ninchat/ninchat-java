
package com.ninchat.swingclient;

import com.ninchat.client.model.*;
import com.ninchat.client.transport.*;
import com.ninchat.client.transport.actions.SendMessage;
import com.ninchat.client.transport.events.Error;
import com.ninchat.client.transport.payloads.NinchatTextMessage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.DateFormat;
import java.util.StringTokenizer;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/*
 * Copyright (c) IRC-Galleria / Dynamoid Oy - 2003
 *               Kari Lavikka <tuner@bdb.fi>
 *
 * $Id: Client.java,v 1.4 2004/03/20 11:30:57 tuner Exp $
 */



public class SwingClient extends JFrame {
    private static final Logger log = Logger.getLogger(ChannelPanel.class.getName());

	public static void main(String [] args) {
		// Setup logging
		ConsoleHandler handler = new ConsoleHandler();
		handler.setLevel(Level.FINEST);
		handler.setFormatter(new SimpleFormatter());
		Logger.getLogger("com.ninchat").addHandler(handler);
		Logger.getLogger("com.ninchat").setLevel(Level.FINEST);

		// Setup transport and session
		WebSocketTransport transport = new WebSocketTransport();
		transport.setHost("api.ninchat.com");
		transport.setWebSocketAdapter(new WeberknechtAdapter());
		Session session = new Session(transport);

		// Create client window
		SwingClient swingClient = new SwingClient(session);
		swingClient.setVisible(true);

		// Display a dialog for credentials. Ok-button handler initiates the connection and login procedure
		CredentialsDialog dialog = new CredentialsDialog(swingClient);
		dialog.pack();
		dialog.setVisible(true);
	}


	Session session;

    JPanel cardPanel;
    CardLayout cardLayout;
    JTextField inputLine;
    JPanel channelButtonPanel;
    Panel activePanel;

    public SwingClient(final Session session) {
        this.session = session;

		session.addSessionListener(new SessionListener() {
			private void newChannel(Channel channel) {
				final ChannelPanel panel = addChannel(channel);
				channel.describeChannel(new SimpleAckListener() {
					@Override
					public void onReady(boolean success) {
						panel.refreshUserList();
					}
				});

				channel.loadHistory(null);
			}

			@Override
			public void onChannelCreated(Session session, Channel channel) {
				newChannel(channel);
			}

			@Override
			public void onAttributesLoaded(Session session) {
				for (Channel channel : session.getChannels().values()) {
					newChannel(channel);
				}
			}

			@Override
			public void onError(Session session, Error error) {
				JOptionPane.showMessageDialog(SwingClient.this,
						String.format("%s - %s", error.getErrorType(), error.getErrorReason()),
						"Error",
						JOptionPane.ERROR_MESSAGE);
			}
		});


        this.setTitle("Ninchat Swing Client");

        this.setSize(800, 400);

        cardLayout = new CardLayout();
        cardPanel = new JPanel();
        cardPanel.setLayout(cardLayout);
        getContentPane().add(cardPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());

        inputLine = new JTextField();
        bottomPanel.add(inputLine, BorderLayout.NORTH);

        inputLine.addKeyListener(
                new KeyAdapter() {
                    final char [] shortcutKeys = {'1', '2', '3', '4', '5', '6', '7', '8', '9', '0',
                                                  'q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p'};

                    public void keyPressed(KeyEvent e) {
                        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                            String input = inputLine.getText();
                            if (input == null || input.length() == 0) return;
                            processInput(input);
                            inputLine.setText(null);

                        } else if ((e.getModifiersEx() & KeyEvent.ALT_DOWN_MASK) == KeyEvent.ALT_DOWN_MASK) {
                            //cat.debug("Alt + something pressed!!");
                            char c = e.getKeyChar();

                            for (int p = 0; p < shortcutKeys.length; p++) {
                                char cp = shortcutKeys[p];
                                if (c == cp) {
                                    //cat.debug("window " + p + " key " + c);

                                    if (p < channelButtonPanel.getComponentCount()) {
                                        ((AbstractButton)channelButtonPanel.getComponent(p)).doClick();
                                    }
                                }
                            }
                        }
                    }
                }
        );


        FlowLayout channelButtonLayout = new FlowLayout();
        channelButtonLayout.setAlignment(FlowLayout.LEFT);
        channelButtonPanel = new JPanel(channelButtonLayout);
        bottomPanel.add(channelButtonPanel, BorderLayout.SOUTH);

        getContentPane().add(bottomPanel, BorderLayout.SOUTH);

    }

	void start(SessionCreationMethod sessionCreationMethod) {
		session.setSessionCreationMethod(sessionCreationMethod);
		session.startSession();
	}

	/**
	 * Send a message to current, visible conversation
	 */
	private void sendTextMessage(String text) {
		if (activePanel instanceof ChannelPanel) {
			ChannelPanel activeChannelPanel = (ChannelPanel)activePanel;

			NinchatTextMessage m = new NinchatTextMessage();
			m.setText(text);

			SendMessage a = new SendMessage();
			a.setChannelId(activeChannelPanel.channel.getId());
			a.setMessageType(NinchatTextMessage.MESSAGE_TYPE);
			a.setPayloads(new Payload[]{m});

			session.getTransport().enqueue(a);
		}
	}

    private void setTopic(Channel channel) {
        setTitle(channel.getName() + " - " + channel.getTopic());
    }

    public void processInput(String input) {

        log.fine("Processing input: " + input);

        if (input.indexOf("/") == 0) {
            StringTokenizer tokenizer = new StringTokenizer(input.substring(1));

            String command = null;
            if (tokenizer.hasMoreTokens()) command = tokenizer.nextToken();


            if (command.equals("join")) {
                if (!tokenizer.hasMoreTokens()) return;
                String channel = tokenizer.nextToken();
//                join(channelListeners);

            } else if (command.equals("part")) {
                if (!tokenizer.hasMoreTokens()) return;
                String channel = tokenizer.nextToken();
//                part(channelListeners);

            } else if (command.equals("leave")) {
                if (!tokenizer.hasMoreTokens()) return;
                String channel = tokenizer.nextToken();
//                part(channelListeners);

            } else if (command.equals("quit")) {
                System.exit(0);
            }

        } else {
			sendTextMessage(input);
        }
    }

    private class ChannelButtonListener implements ActionListener {
        private Panel panel;

        public ChannelButtonListener(Panel panel) {
            this.panel = panel;
        }

        public void actionPerformed(ActionEvent e) {
            cardLayout.show(cardPanel, panel.getName());

            if (activePanel != null) {
                activePanel.channelButton.setSelected(false);
            }

            activePanel = panel;
            activePanel.channelButton.setSelected(true);

            activePanel.channelButton.setState(
                    activePanel.isActive() ?
                    ChannelButton.STATE_NORMAL :
                    ChannelButton.STATE_INACTIVE);

            inputLine.requestFocus();

            if (panel instanceof ChannelPanel) {
                ChannelPanel channelPanel = (ChannelPanel)panel;
                setTopic(channelPanel.channel);
            } else {
                setTitle("Foo");
            }
        }
    }

    private void addPanel(Panel panel, String name) {
        ChannelButton channelButton = new ChannelButton(name);
        panel.channelButton = channelButton;

        cardPanel.add(panel, panel.getName());
        cardPanel.validate();

        channelButton.addActionListener(new ChannelButtonListener(panel));

        channelButtonPanel.add(channelButton);
        channelButtonPanel.validate();

        validate();

		//channelButton.doClick();
    }

    private ChannelPanel addChannel(Channel channel) {
		final ChannelPanel channelPanel = new ChannelPanel(this, channel, session);
		addPanel(channelPanel, channel.getName());
		return channelPanel;
    }

}

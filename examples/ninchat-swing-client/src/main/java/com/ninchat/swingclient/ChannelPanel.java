package com.ninchat.swingclient;


import com.ninchat.client.model.*;
import com.ninchat.client.transport.payloads.NinchatTextMessage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.net.URL;
import java.text.DateFormat;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;


/*
 * Copyright (c) IRC-Galleria / Dynamoid Oy - 2003
 *               Kari Lavikka <tuner@bdb.fi>
 *
 * $Id: ChannelPanel.java,v 1.2 2004/03/20 11:30:57 tuner Exp $
 */


public class ChannelPanel extends Panel {
    private static final Logger log = Logger.getLogger(ChannelPanel.class.getName());

    private final SwingClient swingClient;
    private final Session session;
    final Channel channel;
    private final UserListModel userListModel;
    private final MessageListModel messageListModel;
    private final JScrollPane messageListScrollPane;

    private JList nickList;
    private JList messageList;

    public ChannelPanel(final SwingClient swingClient,
                        final Channel channel,
                        final Session session) {

        this.channel = channel;
        this.swingClient = swingClient;
        this.session = session;

        setLayout(new BorderLayout());

        messageList = new JList();
        messageListModel = new MessageListModel();
        messageList.setModel(messageListModel);
        messageList.setCellRenderer(new MessageCellRenderer());

        messageListScrollPane = new JScrollPane(messageList);
        messageListScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        nickList = new JList();
        nickList.setCellRenderer(new UserCellRenderer());
        JScrollPane nickListScrollPane = new JScrollPane(nickList);
        nickListScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        nickListScrollPane.setPreferredSize(new Dimension(200, 120));

        add(messageListScrollPane, BorderLayout.CENTER);
        add(nickListScrollPane, BorderLayout.EAST);

        userListModel = new UserListModel();
        nickList.setModel(userListModel);

        channel.addConversationListener(new ConversationListener() {
			@Override
			public void onMessage(Conversation conversation, Message message) {
				messageListModel.addMessage(message);
			}

			@Override
			public void onMessages(Conversation conversation, List<Message> messages) {
				messageListModel.addMessages(messages);
			}

			@Override
			public void onMessageUpdated(Conversation conversation, Message message) {

			}
		});

        messageListScrollPane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
            @Override
            public void adjustmentValueChanged(AdjustmentEvent adjustmentEvent) {
                /*
                if (adjustmentEvent.getAdjustable().getValue() == 0)
                    adjustmentEvent.getAdjustable().setValue(1);
                    channel.loadHistory();
                    */
            }
        });
    }


    public String getName() {
        return channel.getName();
    }

    public boolean isActive() {
        return false;
//        return channelListeners.isActive();
    }

	public void refreshUserList() {
		userListModel.refreshAll();
	}

	public void refreshMessageList() {

	}

    private class MessageListModel extends AbstractListModel {
        private List<Message> messages = new ArrayList<Message>();

        private MessageListModel() {  }

        public void addMessage(Message message) {
            if (messages.isEmpty()) {
                messages.add(message);
                fireIntervalAdded(this, 0, 0);

            } else {
                Message first = messages.get(0);
                Message last = messages.get(messages.size() -1);

                if (message.compareTo(first) < 0) {
                    // Before first
                    messages.add(0, message);
                    fireIntervalAdded(this, 0, 0);

                } else if (message.compareTo(last) > 0) {
                    // After last
                    messages.add(message);
                    fireIntervalAdded(this, messages.size() - 1, messages.size() - 1);
                    messageList.ensureIndexIsVisible(messages.size() - 1);

                } else {
                    log.warning("Don't know where to put the message: " + message);
                }
            }
        }

		public void addMessages(Collection<Message> newMessages) {
			// TODO: Implement browsing past messages
			messages.addAll(newMessages);
			fireIntervalAdded(this, 0, messages.size() - 1);
			messageList.ensureIndexIsVisible(messages.size() - 1);
		}

        @Override
        public int getSize() {
            return messages.size();
        }

        @Override
        public Object getElementAt(int i) {
            return messages.get(i);
        }
    }

    private class UserListModel extends AbstractListModel {
        private List<Member> sortedMembers = new ArrayList<Member>();
        private UserComparator comparator = new UserComparator();

        public UserListModel() {

        }

        @Override
        public int getSize() {
            return sortedMembers.size();
        }

        @Override
        public Object getElementAt(int index) {
            return sortedMembers.get(index);
        }

        public void add(Member member) {
            if (sortedMembers.contains(member)) {
                log.warning("SortedMembers already contains " + member);
                return;
            }

            int i = -Collections.binarySearch(sortedMembers, member, comparator) - 1;
            sortedMembers.add(i, member);
        }

        public void remove(Member member) {
            int i = sortedMembers.indexOf(member);
            if (i >= 0) {
                sortedMembers.remove(i);
                fireIntervalRemoved(this, i, i);
            }
        }

        public void update(Member member) {
            remove(member);
            add(member);
       }

        public void refreshAll() {
            if (!sortedMembers.isEmpty()) {
                fireIntervalRemoved(this, 0, sortedMembers.size() - 1);
                sortedMembers.clear();
            }

            for (Map.Entry<String, Member> e : channel.getMembers().entrySet()) {
                User user = e.getValue().getUser();
                Member member = e.getValue();

                sortedMembers.add(member);
            }

            Collections.sort(sortedMembers, comparator);

            if (!sortedMembers.isEmpty()) {
                fireIntervalAdded(this, 0, sortedMembers.size() - 1);
            }
        }

    }

    class UserComparator implements Comparator<Member> {
        @Override
        public int compare(Member a, Member b) {
            int i = (b.getUser().isConnected() ? 1 : 0) - (a.getUser().isConnected() ? 1 : 0);
            if (i != 0) return i;

            i = (b.isOperator() ? 1 : 0) - (a.isOperator() ? 1: 0);
            if (i != 0) return i;

            // TODO: idle

            return a.getUser().getName().compareToIgnoreCase(b.getUser().getName());
        }
    }


    public class UserCellRenderer implements ListCellRenderer {
        public Component getListCellRendererComponent(final JList list,
                                                      final Object value, final int index, final boolean isSelected,
                                                      final boolean cellHasFocus) {

            final Font nameFont = new Font("SansSerif", Font.BOLD, 11);
            final Font realnameFont = new Font("SansSerif", Font.PLAIN, 9);

            return new JPanel() {
                public void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Member member = (Member)value;

                    g.setColor(isSelected ? list.getSelectionBackground() : list
                            .getBackground());
                    g.fillRect(0, 0, getWidth(), getHeight());

                    //URL url = member.getUser().getIconURL();
					g.setColor(Color.LIGHT_GRAY);
					g.drawRect(4, 4, 32, 32);

                    g.setColor(isSelected ? list.getSelectionForeground() : list
                            .getForeground());

                    g.setFont(nameFont);
                    g.drawString(member.getUser().getName(), 40, 15);

                    g.setFont(realnameFont);
	                if (member.getUser().getRealName() != null) {
		                g.drawString(member.getUser().getRealName(), 40, 30);
	                }

                    if (member.getUser().isConnected()) {
                        g.setColor(Color.GREEN);
                        if (member.getUser().getIdle() == null) {
                            g.fillOval(getWidth() - 10, 4, 7, 7);
                        } else {
                            g.drawOval(getWidth() - 10, 4, 7, 7);
                        }
                    }

                    if (member.isOperator()) {
                        g.setColor(Color.ORANGE);
                        g.fillRect(getWidth() - 20, 4, 7, 7);
                    }
                }

                public Dimension getPreferredSize() {
                    return new Dimension(100, 40);
                    /*
                    Font font = (Font) value;
                    String text = font.getFamily();
                    Graphics g = getGraphics();
                    FontMetrics fm = g.getFontMetrics(font);
                    return new Dimension(fm.stringWidth(text), fm.getHeight());
                    */
                }
            };
        }


    }

    public class MessageCellRenderer implements ListCellRenderer {
        final Font nameFont = new Font("SansSerif", Font.BOLD, 11);
        final Font messageFont = new Font("SansSerif", Font.PLAIN, 12);
        final Font timeFont = new Font("SansSerif", Font.PLAIN, 11);

        final DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

        public Component getListCellRendererComponent(final JList list,
                                                      final Object value, final int index, final boolean isSelected,
                                                      final boolean cellHasFocus) {


            return new JPanel() {
                public void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Message message = (Message)value;

                    g.setColor(isSelected ? list.getSelectionBackground() : list
                            .getBackground());
                    g.fillRect(0, 0, getWidth(), getHeight());

                    User user = session.getUser(message.getUserId());

					if (user != null) {
						URL url = user.getIconURL();
						g.setColor(Color.LIGHT_GRAY);
						g.drawRect(4, 4, 32, 32);

					} else {
						log.fine("User #" + message.getUserId() + " was not found!");
					}

					/*
                    g.setColor(isSelected ? list.getSelectionForeground() : list
                            .getForeground());
                            */

                    g.setColor(Color.BLACK);
                    g.setFont(nameFont);
	                g.drawString(message.getUserName() != null ? message.getUserName() : "null", 40, 15);

					g.setFont(messageFont);
					if (message.getPayload() instanceof NinchatTextMessage) {
                        g.drawString(((NinchatTextMessage) message.getPayload()).getText(), 40, 30);

                    } else if (message.getPayload() != null) {
						g.setColor(Color.GRAY);
						g.drawString("-- payload type not supported by Swing client: " + message.getPayload().getClass().getSimpleName() + " --", 40, 30);
					}

                    String formattedTime = df.format(message.getTime());

                    g.setColor(Color.GRAY);
                    g.setFont(timeFont);
                    FontMetrics fm = g.getFontMetrics(timeFont);

                    g.drawString(formattedTime, getWidth() - fm.stringWidth(formattedTime) - 5, 15);


                }

                public Dimension getPreferredSize() {
                    return new Dimension(300, 40);
                    /*
                    Font font = (Font) value;
                    String text = font.getFamily();
                    Graphics g = getGraphics();
                    FontMetrics fm = g.getFontMetrics(font);
                    return new Dimension(fm.stringWidth(text), fm.getHeight());
                    */
                }
            };
        }


    }
}

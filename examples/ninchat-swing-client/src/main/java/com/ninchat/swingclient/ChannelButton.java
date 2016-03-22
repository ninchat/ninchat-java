package com.ninchat.swingclient;

import javax.swing.*;
import java.awt.*;

/*
 * Copyright (c) IRC-Galleria / Dynamoid Oy - 2003
 *               Kari Lavikka <tuner@bdb.fi>
 *
 * $Id: ChannelButton.java,v 1.1 2004/03/04 16:37:51 tuner Exp $
 */



public class ChannelButton extends JToggleButton {
    public static final int STATE_INACTIVE = 0;
    public static final int STATE_NORMAL = 1;
    public static final int STATE_COMMAND = 2;
    public static final int STATE_MESSAGE = 3;
    public static final int STATE_PERSONAL = 4;

    private int state = 1;

    private static Color stateColors[][] = {{new Color(0x80, 0x80, 0x80), null},
                                            {null, null},
                                            {new Color(0x60, 0x00, 0x00), null},
                                            {new Color(0xB0, 0x00, 0x00), null},
                                            {Color.BLACK, new Color(0x80, 0xFF, 0x80)}};

    public void setState(int state) {
        if (state > 1 && state <= this.state) return;
        this.state = state;

        this.setForeground(stateColors[state][0]);
        this.setBackground(stateColors[state][1]);
    }

    public ChannelButton() {
        super();
    }

    public ChannelButton(String name) {
        super(name);
    }
}

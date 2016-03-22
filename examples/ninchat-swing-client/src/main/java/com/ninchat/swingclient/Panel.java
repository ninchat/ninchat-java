
package com.ninchat.swingclient;
import javax.swing.*;

/*
 * Copyright (c) IRC-Galleria / Dynamoid Oy - 2003
 *               Kari Lavikka <tuner@bdb.fi>
 *
 * $Id: Panel.java,v 1.1 2004/03/04 16:37:51 tuner Exp $
 */


public abstract class Panel extends JPanel {
    protected ChannelButton channelButton;

    public abstract String getName();

    public abstract boolean isActive();

}

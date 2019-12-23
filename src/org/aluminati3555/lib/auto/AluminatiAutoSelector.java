/**
 * Copyright (c) 2019 Team 3555
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.aluminati3555.lib.auto;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;

import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.Timer;

/**
 * This class manages autonomous mode selection
 * 
 * @author Caleb Heydon
 */
public class AluminatiAutoSelector extends Thread {
    private int port;
    private ArrayList<Entry> entries;

    private DatagramSocket socket;
    private DatagramPacket packet;

    private AluminatiAutoTask autoMode;

    @Override
    public String toString() {
        return "[AutoSelector] port: " + port;
    }

    /**
     * Publishes the auto modes to network tables
     */
    private void publish() {
        String[] modes = new String[entries.size()];
        for (int i = 0; i < entries.size(); i++) {
            modes[i] = entries.get(i).name;
        }

        NetworkTableInstance.getDefault().getTable("SmartDashboard").getEntry("Auto List").setStringArray(modes);
    }

    /**
     * Selects an auto mode
     */
    private void select(String mode) {
        synchronized (this) {
            boolean found = false;
            for (int i = 0; i < entries.size(); i++) {
                if (entries.get(i).name.equals(mode)) {
                    autoMode = entries.get(i).mode;
                    found = true;
                }
            }

            if (!found) {
                autoMode = null;
            }
        }
    }

    /**
     * Returns the selected mode
     * 
     * @return
     */
    public AluminatiAutoTask getSelected() {
        synchronized (this) {
            if (autoMode != null) {
                return autoMode;
            }

            String auto = NetworkTableInstance.getDefault().getTable("SmartDashboard").getEntry("Auto Selector")
                    .getString(null);

            if (auto == null) {
                return null;
            }

            select(auto);
            return autoMode;
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                socket.receive(packet);

                ByteArrayInputStream byteInput = new ByteArrayInputStream(packet.getData());
                DataInputStream input = new DataInputStream(byteInput);

                String data = input.readUTF();
                input.close();

                select(data);
            } catch (IOException e) {
                Timer.delay(1);
                continue;
            }
        }
    }

    public AluminatiAutoSelector(int port, Entry... entries) {
        this.port = port;

        this.entries = new ArrayList<Entry>();
        for (int i = 0; i < entries.length; i++) {
            this.entries.add(entries[i]);
        }

        publish();
    }

    public static class Entry {
        public String name;
        public AluminatiAutoTask mode;

        public Entry(String name, AluminatiAutoTask mode) {
            this.name = name;
            this.mode = mode;
        }
    }
}

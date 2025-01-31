package org.example;

import com.jcraft.jsch.*;

public class SFTPConnection {
    private Session session;
    private ChannelSftp channelSftp;

    // Установка соединения с sftp-сервером
    public void connect(String host, int port, String username, String password) throws JSchException {
        JSch jsch = new JSch();
        session = jsch.getSession(username, host, port);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();

        channelSftp = (ChannelSftp) session.openChannel("sftp");
        channelSftp.connect();
    }

    public ChannelSftp getChannelSftp() {
        return channelSftp;
    }

    // Разрыв соединения с sftp-сервером
    public void disconnect() {
        if (channelSftp != null && channelSftp.isConnected()) {
            channelSftp.disconnect();
        }
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }
}
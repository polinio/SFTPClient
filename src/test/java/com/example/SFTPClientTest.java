package com.example;

import com.jcraft.jsch.*;
import org.example.SFTPClient;
import org.example.SFTPConnection;
import org.testng.Assert;
import org.testng.annotations.*;
import static org.testng.Assert.*;

import java.io.InputStream;
import java.util.Properties;

public class SFTPClientTest {

    private SFTPConnection sftpConnection;
    private ChannelSftp channelSftp;
    private String host;
    private int port;
    private String username;
    private String password;

    @BeforeClass
    public void loadConfig() {
        // загрузка конфигурации для подключения к серверу
        try (InputStream input = SFTPClientTest.class.getClassLoader().getResourceAsStream("config.properties")) {
            Properties prop = new Properties();
            prop.load(input);

            host = prop.getProperty("sftp.host");
            port = Integer.parseInt(prop.getProperty("sftp.port"));
            username = prop.getProperty("sftp.username");
            password = prop.getProperty("sftp.password");

            System.out.println(("Конфигурация загружена: " + host + ":" + port));
        } catch (Exception e) {
            System.err.println(("Ошибка загрузки конфигурации: " + e.getMessage()));
            throw new RuntimeException("Не удалось загрузить конфигурацию", e);
        }
    }

    @BeforeMethod
    public void setUp() throws JSchException {
        sftpConnection = new SFTPConnection();
        sftpConnection.connect(host, port, username, password);
        channelSftp = sftpConnection.getChannelSftp();
    }

    @Test (description = "Проверка успешного подключения к SFTP-серверу")
    public void testConnectSuccess() {
        Assert.assertTrue(channelSftp.isConnected(), "SFTP-канал должен быть подключен");
    }

    /* --- ТЕСТЫ getIPByDomain --- */

    @Test (description = "Проверка получения IP по домену")
    public void testGetIPByDomainSuccess() throws SftpException {
        String ip = SFTPClient.getIPByDomain(channelSftp, "first.domain");
        Assert.assertEquals(ip, "192.168.0.1", "IP должен соответствовать домену first.domain");
    }

    @Test  (description = "Проверка получения IP по домену (регистронезависимость)")
    public void testGetIPByDomainCaseInsensitive() throws SftpException {
        String ip = SFTPClient.getIPByDomain(channelSftp, "FIRST.DOMAIN");
        Assert.assertEquals(ip, "192.168.0.1", "IP должен соответствовать домену first.domain (регистр не должен влиять)");
    }

    @Test (description = "Проверка получения IP по несуществующему домену")
    public void testGetIPByDomainNotFound() throws SftpException {
        String ip = SFTPClient.getIPByDomain(channelSftp, "nonexistent.domain");
        Assert.assertEquals(ip, "Domain не найден!", "Должно вернуться сообщение 'Domain не найден!'");
    }

    /* --- ТЕСТЫ getDomainByIP --- */

    @Test (description = "Проверка получения домена по IP")
    public void testGetDomainByIPSuccess() throws SftpException {
        String domain = SFTPClient.getDomainByIP(channelSftp, "192.168.0.1");
        Assert.assertEquals(domain, "first.domain", "Домен должен соответствовать IP 192.168.0.1");
    }

    @Test (description = "Проверка получения домена по IP (разный формат IP)")
    public void testGetDomainByIPCaseInsensitive() throws SftpException {
        String domain = SFTPClient.getDomainByIP(channelSftp, "192.168.000.001");
        Assert.assertEquals(domain, "first.domain", "Домен должен соответствовать IP 192.168.0.1 (формат IP не должен влиять)");
    }

    @Test (description = "Проверка получения домена по несуществующему IP")
    public void testGetDomainByIPNotFound() throws SftpException {
        String domain = SFTPClient.getDomainByIP(channelSftp, "10.0.0.1");
        Assert.assertEquals(domain, "IP не найден!", "Должно вернуться сообщение 'IP не найден!'");
    }

    /* --- ТЕСТЫ isValidIP --- */

    @Test (description = "Проверка валидных IP-адресов")
    public void testIsValidIPSuccess() {
        assertTrue(SFTPClient.isValidIP("192.168.0.1"), "192.168.0.1 должен быть валидным IP");
        assertTrue(SFTPClient.isValidIP("10.0.0.1"), "10.0.0.1 должен быть валидным IP");
        assertTrue(SFTPClient.isValidIP("255.255.255.255"), "255.255.255.255 должен быть валидным IP");
        assertTrue(SFTPClient.isValidIP("0.0.0.0"), "0.0.0.0 должен быть валидным IP");
    }

    @Test (description = "Проверка невалидных IP-адресов")
    public void testIsValidIPFailure() {
        assertFalse(SFTPClient.isValidIP("256.256.256.256"), "256.256.256.256 не должен быть валидным IP");
        assertFalse(SFTPClient.isValidIP("192.168.0"), "192.168.0 не должен быть валидным IP");
        assertFalse(SFTPClient.isValidIP("192.168.0.1.1"), "192.168.0.1.1 не должен быть валидным IP");
        assertFalse(SFTPClient.isValidIP("192.168.0.256"), "192.168.0.256 не должен быть валидным IP");
        assertFalse(SFTPClient.isValidIP("192.168.0.-1"), "192.168.0.-1 не должен быть валидным IP");
    }

    @Test (description = "Проверка пустого IP")
    public void testIsValidIPEmpty() {
        assertFalse(SFTPClient.isValidIP(""), "Пустая строка не должна быть валидным IP");
    }

    @Test (description = "Проверка строки в неправильном формате")
    public void testIsValidIPInvalidFormat() {
        assertFalse(SFTPClient.isValidIP("192.168.0"), "192.168.0 не должен быть валидным IP");
        assertFalse(SFTPClient.isValidIP("192.168.0.1.1"), "192.168.0.1.1 не должен быть валидным IP");
        assertFalse(SFTPClient.isValidIP("192.168.0."), "192.168.0. не должен быть валидным IP");
        assertFalse(SFTPClient.isValidIP(".192.168.0.1"), ".192.168.0.1 не должен быть валидным IP");
        assertFalse(SFTPClient.isValidIP("192,168,0,1"), "192,168,0,1 не должен быть валидным IP");
    }

    /* --- ТЕСТЫ addPair --- */

    @Test (description = "Проверка добавления domain-IP пары")
    public void testAddPairSuccess() throws SftpException {
        SFTPClient.addPair(channelSftp, "fourth.domain", "192.168.0.4");
        String ip = SFTPClient.getIPByDomain(channelSftp, "fourth.domain");
        assertEquals(ip, "192.168.0.4", "IP address should match");
    }

    @Test (description = "Проверка добавления дубликата домена")
    public void testAddPairDuplicateDomain() throws SftpException {
        // добавление первой пары
        SFTPClient.addPair(channelSftp, "duplicatedomain.com", "192.168.0.101");
        // попытка добавить дубликат домена
        SFTPClient.addPair(channelSftp, "duplicatedomain.com", "192.168.0.102");
        // проверка на то, что IP остался прежним
        String ip = SFTPClient.getIPByDomain(channelSftp, "duplicatedomain.com");
        Assert.assertEquals(ip, "192.168.0.101", "IP должен остаться прежним (дубликат домена не добавлен)");
    }

    @Test (description = "Проверка добавления дубликата IP")
    public void testAddPairDuplicateIP() throws SftpException {
        // добавление первой пары
        SFTPClient.addPair(channelSftp, "domain1.com", "192.168.0.103");
        // попытка добавить дубликат IP
        SFTPClient.addPair(channelSftp, "domain2.com", "192.168.0.103");
        // проверка на то, что домен остался прежним
        String domain = SFTPClient.getDomainByIP(channelSftp, "192.168.0.103");
        Assert.assertEquals(domain, "domain1.com", "Домен должен остаться прежним (дубликат IP не добавлен)");
    }

    @Test (description = "Проверка добавления пары с невалидным IP")
    public void testAddPairInvalidIP() throws SftpException {
        // попытка добавить пару с невалидным IP
        SFTPClient.addPair(channelSftp, "invalidip.com", "999.999.999.999");
        // проверка на то, что пара не была добавлена
        String ip = SFTPClient.getIPByDomain(channelSftp, "invalidip.com");
        Assert.assertEquals(ip, "Domain не найден!", "Пара с невалидным IP не должна быть добавлена");
    }

    @Test (description = "Проверка добавления пары с пустым доменом")
    public void testAddPairEmptyDomain() throws SftpException {
        // попытка добавить пару с пустым доменом
        SFTPClient.addPair(channelSftp, "", "192.168.0.104");
        // проверка на то, что пара не была добавлена
        String ip = SFTPClient.getIPByDomain(channelSftp, "");
        Assert.assertEquals(ip, "Domain не найден!", "Пара с пустым доменом не должна быть добавлена");
    }

    @Test (description = "Проверка добавления пары с пустым IP")
    public void testAddPairEmptyIP() throws SftpException {
        // попытка добавить пару с пустым доменом IP
        SFTPClient.addPair(channelSftp, "emptyip.com", "");
        // проверка на то, что пара не была добавлена
        String ip = SFTPClient.getIPByDomain(channelSftp, "emptyip.com");
        Assert.assertEquals(ip, "Domain не найден!", "Пара с пустым IP не должна быть добавлена");
    }

    /* --- ТЕСТЫ deletePair --- */

    @Test (description = "Проверка удаления пары")
    public void testDeletePairSuccess() throws SftpException {
        // удаление пары по домену
        SFTPClient.addPair(channelSftp, "example.com", "192.168.0.5");
        SFTPClient.deletePair(channelSftp, "example.com");
        // проверка на то, что пара была удалена
        String ipAfterDeleteByDomain = SFTPClient.getIPByDomain(channelSftp, "example.com");
        assertEquals(ipAfterDeleteByDomain, "Domain не найден!", "Domain должен быть удален");
        // удаление пары по IP
        SFTPClient.addPair(channelSftp, "example.com", "192.168.0.5");
        SFTPClient.deletePair(channelSftp, "192.168.0.5");
        // проверка на то, что пара была удалена
        String domainAfterDeleteByIP = SFTPClient.getDomainByIP(channelSftp, "192.168.0.5");
        assertEquals(domainAfterDeleteByIP, "IP не найден!", "IP should be deleted");
    }

    @AfterMethod
    public void tearDown() {
        sftpConnection.disconnect();
    }
}
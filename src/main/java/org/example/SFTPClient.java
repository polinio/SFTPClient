package org.example;

import com.jcraft.jsch.*;

import java.io.*;
import java.util.*;

public class SFTPClient {
    private static final SFTPConnection sftpConnection = new SFTPConnection();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // ввод данных для подключения к SFTP-серверу
        System.out.print("Адрес: ");
        String host = scanner.nextLine();
        System.out.print("Порт: ");
        int port = Integer.parseInt(scanner.nextLine());
        System.out.print("Логин: ");
        String username = scanner.nextLine();
        System.out.print("Пароль: ");
        String password = scanner.nextLine();

        try {
            sftpConnection.connect(host, port, username, password);
            System.out.println("Успешное подключение к серверу!");

            showMenu(scanner);

        } catch (Exception e) {
            System.err.println("Ошибка: " + e);
        } finally {
            sftpConnection.disconnect();
        }
    }

    private static void showMenu(Scanner scanner) {
        ChannelSftp channelSftp = null;
        try {
            channelSftp = sftpConnection.getChannelSftp();

            while (true) {
                System.out.println("\nВыберите действие:");
                System.out.println("1. Получить domain-IP пары");
                System.out.println("2. Получить IP по domain");
                System.out.println("3. Получить domain по IP");
                System.out.println("4. Добавить новую domain-IP пару");
                System.out.println("5. Удалить domain-IP пару");
                System.out.println("6. Выход");

                String c = scanner.nextLine();
                switch (c) {
                    case "1":
                        List<Map<String, String>> pairs = getDomainIPPairs(channelSftp);
                        pairs.sort(Comparator.comparing(p -> p.get("domain")));
                        pairs.forEach(pair ->
                                System.out.println("Domain: " + pair.get("domain") + ", IP: " + pair.get("ip")));
                        break;
                    case "2":
                        System.out.println("Введите domain для получения IP: ");
                        String domain = scanner.nextLine();
                        System.out.println("IP: " + getIPByDomain(channelSftp, domain));
                        break;
                    case "3":
                        System.out.println("Введите IP для получения domain: ");
                        String ip = scanner.nextLine();
                        System.out.println("domain: " + getDomainByIP(channelSftp, ip));
                        break;
                    case "4":
                        System.out.println("Введите IP : ");
                        String addIp = scanner.nextLine();
                        System.out.println("Введите domain: ");
                        String addDomain = scanner.nextLine();
                        addPair(channelSftp,addDomain,addIp);
                        break;
                    case "5":
                        System.out.println("Введите IP или domain для удаления пары : ");
                        String data = scanner.nextLine();
                        deletePair(channelSftp,data);
                        break;
                    case "6":
                        System.out.println("Выход...");
                        return;
                    default:
                        System.out.println("Неверное действие!");
                }
            }
        } catch (SftpException e) {
            System.err.println("Err" + e);
        } finally {
            if (channelSftp != null && channelSftp.isConnected())
                channelSftp.disconnect();
        }
    }

    // Получение domain-IP пары
    private static List<Map<String, String>> getDomainIPPairs(ChannelSftp channelSftp) throws SftpException {
        String content = getFileContent(channelSftp);
        List<Map<String, String>> pairs = new ArrayList<>();

        if (!content.trim().isEmpty() && content.contains("addresses")) {
            // регулярное выражение, позволяющее пробелы между ":" и "["
            String[] parts = content.split(":\\s*\\[");
            if (parts.length < 2) {
                System.err.println("Неверный формат JSON: отсутствует ожидаемый разделитель \":[\".");
                return pairs;
            }
            String jsonArrayPart = parts[1];
            String[] closingBracketSplit = jsonArrayPart.split("]");
            if (closingBracketSplit.length < 1) {
                System.err.println("Неверный формат JSON: отсутствует закрывающая скобка ']'.");
                return pairs;
            }
            String jsonArray = closingBracketSplit[0].replaceAll("\\s+", ""); // взятие массива JSON
            String[] entries = jsonArray.split("},\\{"); // разделение пар

            for (String entry : entries) {
                entry = entry.replaceAll("[{}\"]", ""); // удаление фигурных скобок и кавычек
                String[] fields = entry.split(",");

                Map<String, String> pair = new HashMap<>();
                for (String field : fields) {
                    String[] keyValue = field.split(":");
                    if (keyValue.length == 2) {
                        pair.put(keyValue[0].trim(), keyValue[1].trim());
                    } else {
                        System.err.println("Неверный формат пары: " + field);
                    }
                }
                pairs.add(pair);
            }
        }
        return pairs;
    }

    // Поиск json-файла на сервере
    private static String findJsonFile(ChannelSftp channelSftp) throws SftpException {
        // получение списка файлов в текущей директории
        @SuppressWarnings("unchecked")
        Vector<ChannelSftp.LsEntry> files = channelSftp.ls(".");
        for (ChannelSftp.LsEntry entry : files) {
            String filename = entry.getFilename();
            // пропуск служебных записей "." и ".."
            if (!".".equals(filename) && !"..".equals(filename)
                    && filename.toLowerCase().endsWith(".json")) {
                return filename;
            }
        }
        throw new SftpException(0, "JSON файл не найден");
    }

    // Чтение файла
    private static String getFileContent(ChannelSftp channelSftp) throws SftpException {
        String jsonFileName = findJsonFile(channelSftp);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(); // запись на временное хранение в виде байт
        channelSftp.get(jsonFileName, outputStream); // запись в поток из файла
        return outputStream.toString();
    }

    // Получение IP по domain
    public static String getIPByDomain(ChannelSftp channelSftp, String domain) throws SftpException{
        List<Map<String,String>> pairs = getDomainIPPairs(channelSftp);
        return pairs.stream()
                .filter(pair -> domain.equalsIgnoreCase(pair.get("domain")))
                .map(pair -> pair.get("ip"))
                .findFirst()
                .orElse("Domain не найден!");
    }

    // Получение domain по IP
    public static String getDomainByIP(ChannelSftp channelSftp, String ip) throws SftpException{
        List<Map<String,String>> pairs = getDomainIPPairs(channelSftp);
        String normalizedIP = normalizeIP(ip); // Нормализация IP-адреса
        return pairs.stream()
                .filter(pair -> normalizedIP.equals(normalizeIP(pair.get("ip")))) // Сравнение нормализованных IP
                .map(pair -> pair.get("domain"))
                .findFirst()
                .orElse("IP не найден!");
    }

    // Нормализация IP-адреса (удаление ведущих нулей)
    private static String normalizeIP(String ip) {
        if (ip == null || ip.isEmpty()) {
            return ip;
        }
        String[] octets = ip.split("\\.");
        StringBuilder normalizedIP = new StringBuilder();
        for (String octet : octets) {
            normalizedIP.append(Integer.parseInt(octet)).append(".");
        }
        return normalizedIP.substring(0, normalizedIP.length() - 1); // удаление последней точки
    }

    // Добавление новой domain-IP пары
    public static void addPair(ChannelSftp channelSftp, String domain, String ip) throws SftpException {
        // проверка на пустой домен
        if (domain == null || domain.trim().isEmpty()) {
            System.out.println("Домен не может быть пустым");
            return;
        }

        // проверка на невалидный ip
        if (!isValidIP(ip)){
            System.out.println("Недопустимый IP: " + ip);
            return;
        }

        List<Map<String,String>> pairs = getDomainIPPairs(channelSftp);

        // проверка на дубликаты
        if (pairs.stream()
                .anyMatch(pair -> domain.equals(pair.get("domain")) || ip.equals(pair.get("ip")))){
            System.out.println("Domain или IP уже существует");
            return;
        }

        Map<String,String> newPair = new HashMap<>();
        newPair.put("domain", domain);
        newPair.put("ip", ip);
        pairs.add(newPair);
        savePairs(channelSftp,pairs);
    }

    // Удаление domain-IP пары
    public static void deletePair(ChannelSftp channelSftp, String data) throws SftpException {
        List<Map<String,String>> pairs = getDomainIPPairs(channelSftp);
        pairs.removeIf(pair -> data.equals(pair.get("domain")) || data.equals(pair.get("ip")));
        savePairs(channelSftp, pairs);
    }

    // Проверка валидности IPv4
    public static boolean isValidIP(String ip){
        return ip.matches("^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
    }

    // Сохранение пар
    private static void savePairs(ChannelSftp channelSftp, List<Map<String, String>> pairs) throws SftpException {
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{\"addresses\":[");

        for (int i = 0; i < pairs.size(); i++) {
            Map<String, String> pair = pairs.get(i);
            jsonBuilder.append("{\"domain\":\"").append(pair.get("domain")).append("\",")
                    .append("\"ip\":\"").append(pair.get("ip")).append("\"}");
            if (i < pairs.size() - 1) {
                jsonBuilder.append(",");
            }
        }

        jsonBuilder.append("]}");

        String jsonFileName = findJsonFile(channelSftp);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(jsonBuilder.toString().getBytes());
        try {
            channelSftp.put(inputStream, jsonFileName, ChannelSftp.OVERWRITE);
            System.out.println("Файл обновлен!");
        } catch (SftpException e) {
            System.err.println("Ошибка при записи файла на сервер: " + e.getMessage());
            throw e;
        }
    }
}
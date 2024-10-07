package org.example;
import java.math.BigDecimal;
import java.sql.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

public class Main {
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/auth_db";
    private static final String USER = "postgres";
    private static final String PASS = "admin";

    private static final String[] POSITIONS = {"M", "A", "E"};
    private static int lastId = 1;

    private static final Map<Character, String> cyrillicToEnglish = new HashMap<>();

    static {
        cyrillicToEnglish.put('а', "a");
        cyrillicToEnglish.put('б', "b");
        cyrillicToEnglish.put('в', "v");
        cyrillicToEnglish.put('г', "g");
        cyrillicToEnglish.put('д', "d");
        cyrillicToEnglish.put('е', "e");
        cyrillicToEnglish.put('ё', "e");
        cyrillicToEnglish.put('ж', "zh");
        cyrillicToEnglish.put('з', "z");
        cyrillicToEnglish.put('и', "i");
        cyrillicToEnglish.put('й', "y");
        cyrillicToEnglish.put('к', "k");
        cyrillicToEnglish.put('л', "l");
        cyrillicToEnglish.put('м', "m");
        cyrillicToEnglish.put('н', "n");
        cyrillicToEnglish.put('о', "o");
        cyrillicToEnglish.put('п', "p");
        cyrillicToEnglish.put('р', "r");
        cyrillicToEnglish.put('с', "s");
        cyrillicToEnglish.put('т', "t");
        cyrillicToEnglish.put('у', "u");
        cyrillicToEnglish.put('ф', "f");
        cyrillicToEnglish.put('х', "kh");
        cyrillicToEnglish.put('ц', "ts");
        cyrillicToEnglish.put('ч', "ch");
        cyrillicToEnglish.put('ш', "sh");
        cyrillicToEnglish.put('щ', "shch");
        cyrillicToEnglish.put('ъ', "");
        cyrillicToEnglish.put('ы', "y");
        cyrillicToEnglish.put('ь', "");
        cyrillicToEnglish.put('э', "e");
        cyrillicToEnglish.put('ю', "yu");
        cyrillicToEnglish.put('я', "ya");
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASS)) {
            System.out.println("Выберите действие: 1 - Войти, 2 - Регистрация");
            int choice = scanner.nextInt();
            scanner.nextLine();

            if (choice == 1) {
                login(scanner, connection);
            } else if (choice == 2) {
                register(scanner, connection);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private static void login(Scanner scanner, Connection connection) throws SQLException {
        System.out.print("Введите логин: ");
        String login = scanner.nextLine();
        System.out.print("Введите пароль: ");
        String password = scanner.nextLine();
        String passwordHash = hashPassword(password);

        String sql = "SELECT * FROM auth_users WHERE login = ? AND password = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, login);
            pstmt.setString(2, passwordHash);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String name = rs.getString("name");
                String position = rs.getString("position");
                greetUser(name);
                System.out.println("Добро пожаловать, " + name + ". Ваша зарплата: " + rs.getBigDecimal("salary"));
            } else {
                System.out.println("Неправильный логин или пароль.");
            }
        }
    }

    private static void register(Scanner scanner, Connection connection) throws SQLException {
        System.out.print("Введите фамилию: ");
        String patronymic = scanner.nextLine();
        System.out.print("Введите имя: ");
        String name = scanner.nextLine();
        System.out.print("Введите отчество: ");
        String surname = scanner.nextLine();
        System.out.print("Введите пароль: ");
        String password = scanner.nextLine();
        String passwordHash = hashPassword(password);

        String position = assignPosition(scanner);
        String id = generateID(position);
        BigDecimal salary = generateSalary(position);
        String login = generateLogin(patronymic, name, surname);

        String sql = "INSERT INTO auth_users (id, name, surname, patronymic, salary, login, password, position) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, id);
            pstmt.setString(2, name);
            pstmt.setString(3, surname);
            pstmt.setString(4, patronymic);
            pstmt.setBigDecimal(5, salary);
            pstmt.setString(6, login);
            pstmt.setString(7, passwordHash);
            pstmt.setString(8, position);
            pstmt.executeUpdate();
            System.out.println("Регистрация успешна! Ваш логин: " + login);
        }
    }
    private static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static String generateID(String position) {
        String prefix;
        switch (position.toLowerCase()) {
            case "manager":
                prefix = POSITIONS[0]; // M
                break;
            case "administrator":
                prefix = POSITIONS[1]; // A
                break;
            case "engineer":
                prefix = POSITIONS[2]; // E
                break;
            default:
                throw new IllegalArgumentException("Неизвестная должность: " + position);
        }

        String idNumber = String.format("%06d", lastId++);
        return prefix + "-" + idNumber;
    }
    private static String generateLogin(String lastName, String firstName, String middleName) {
        String englishLastName = convertToEnglish(lastName);
        String initialFirstName = firstName.toLowerCase().charAt(0) + ".";
        String initialMiddleName = middleName.toLowerCase().charAt(0) + ".";

        return (englishLastName + "." + initialFirstName + initialMiddleName).toLowerCase();
    }

    private static String convertToEnglish(String cyrillic) {
        StringBuilder english = new StringBuilder();
        for (char ch : cyrillic.toLowerCase().toCharArray()) {
            english.append(cyrillicToEnglish.getOrDefault(ch,""));
        }
        return english.toString();
    }

    private static BigDecimal generateSalary(String position) {
        Random rand = new Random();
        switch (position.toLowerCase()) {
            case "manager":
                return BigDecimal.valueOf(50000 + rand.nextInt(40001));
            case "administrator":
                return BigDecimal.valueOf(60000 + rand.nextInt(60001));
            case "engineer":
                return BigDecimal.valueOf(70000 + rand.nextInt(30001));
            default:
                return BigDecimal.ZERO;
        }
    }

    private static String assignPosition(Scanner scanner) {
        System.out.println("Выберите должность: ");
        System.out.println("1 - Менеджер");
        System.out.println("2 - Администратор");
        System.out.println("3 - Инженер");

        String choice = scanner.nextLine();

        switch (choice){
            case "1":
                return "manager";
            case "2":
                return "administrator";
            case "3":
                return "engineer";
            default:
                System.out.println("Неправильный выбор, по умолчанию назначается 'Инженер'.");
                return "engineer";
        }
    }

    private static void greetUser(String name) {
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour >= 4 && hour < 12) {
            greeting = "Доброе утро";
        } else if (hour >= 12 && hour < 17) {
            greeting = "Добрый день";
        } else if (hour >= 17 && hour < 22) {
            greeting = "Добрый вечер";
        } else {
            greeting = "Доброй ночи";
        }
        System.out.println(greeting + ", " + name + "!");
    }
}
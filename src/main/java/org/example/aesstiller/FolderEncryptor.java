package org.example.aesstiller;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Arrays;
import java.util.Scanner;
import java.security.SecureRandom;

public class FolderEncryptor {
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CFB8/PKCS5Padding";

    public static void main(String[] ignoredArgs) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("Шифрування файлів та папок");
            System.out.println("1. Введіть назву файлу або папки для шифрування");
            System.out.println("2. Вийти");
            System.out.print("Виберіть опцію: ");

            String option = scanner.nextLine();

            switch (option) {
                case "1" -> {
                    System.out.print("Введіть назву файлу або папки: ");
                    String path = scanner.nextLine();
                    System.out.print("Введіть пароль: ");
                    String pass = scanner.nextLine();
                    handleFileOrDirectory(path, pass);
                }
                case "2" -> {
                    System.out.println("Вихід з програми.");
                    scanner.close();
                    System.exit(0);
                }
                default -> System.out.println("Невірна опція. Спробуйте ще раз.");
            }
        }
    }

    private static void handleFileOrDirectory(String path, String pass) {
        File fileOrDir = new File(path);

        if (!fileOrDir.exists()) {
            System.out.println("Файл або папка не існує. Перевірте правильність введеного шляху.");
            return;
        }

        if (fileOrDir.isFile()) {
            encryptFile(fileOrDir, pass);
            System.out.println("Зашифрований файл: " + fileOrDir.getAbsolutePath() + ".hidden");

        } else if (fileOrDir.isDirectory()) {
            encryptFolder(fileOrDir, pass);
            System.out.println("Зашифрована папка: " + fileOrDir.getAbsolutePath());
        }
    }

    public static void encryptFolder(File folder, String password) {
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        encryptFolder(file, password);
                    } else {
                        encryptFile(file, password);
                    }
                }
            }
        }
    }

    public static void decryptFolder(File folder, String password) {
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        decryptFolder(file, password);
                    } else {
                        decryptFile(file, password);
                    }
                }
            }
        }
    }

    public static File encryptFile(File file, String password) {
        try {
            FileInputStream inputStream = new FileInputStream(file);
            byte[] fileBytes = new byte[(int) file.length()];
            inputStream.read(fileBytes);
            inputStream.close();

            SecureRandom secureRandom = new SecureRandom();
            byte[] iv = new byte[16];
            secureRandom.nextBytes(iv);

            byte[] encryptedBytes = encrypt(fileBytes, password, iv);

            File encryptedFile = new File(file.getAbsolutePath() + ".hidden");
            FileOutputStream outputStream = new FileOutputStream(encryptedFile);
            outputStream.write(iv);
            assert encryptedBytes != null;
            outputStream.write(encryptedBytes);
            outputStream.close();

            if (!file.delete()) {
                System.err.println("Failed to delete file: " + file.getAbsolutePath());
            }
            return encryptedFile;
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    public static File decryptFile(File file, String password) {
        try {
            FileInputStream inputStream = new FileInputStream(file);
            byte[] iv = new byte[16];
            inputStream.read(iv);

            byte[] fileBytes = new byte[(int) file.length() - 16];
            inputStream.read(fileBytes);
            inputStream.close();

            byte[] decryptedBytes = decrypt(fileBytes, password, iv);

            File decryptedFile = new File(file.getAbsolutePath().replace(".hidden", ""));
            FileOutputStream outputStream = new FileOutputStream(decryptedFile);
            assert decryptedBytes != null;
            outputStream.write(decryptedBytes);
            outputStream.close();

            if (!file.delete()) {
                System.err.println("Failed to delete file: " + file.getAbsolutePath());
            }
            return decryptedFile;
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    private static byte[] encrypt(byte[] input, String password, byte[] iv) {
        try {
            Key secretKey = generateKey(password);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));
            return cipher.doFinal(input);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    private static byte[] decrypt(byte[] input, String password, byte[] iv) {
        try {
            Key secretKey = generateKey(password);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
            return cipher.doFinal(input);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    private static Key generateKey(String password) {
        try {
            byte[] passwordBytes = Arrays.copyOf(password.getBytes(StandardCharsets.UTF_8), 32); // 32 bytes for AES-256
            return new SecretKeySpec(passwordBytes, ALGORITHM);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return null;
    }
}

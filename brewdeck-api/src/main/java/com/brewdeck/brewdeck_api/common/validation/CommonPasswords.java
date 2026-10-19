package com.brewdeck.brewdeck_api.common.validation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * The local blocklist in {@code security/common-passwords.txt}: common passwords of 15 to 128
 * characters from SecLists, lowercased (ADR-018). Loaded once, on first use. Matching ignores case
 * and Unicode normalization form, so "PasswordPassword" is as blocked as "passwordpassword".
 */
public final class CommonPasswords {

  private static final String RESOURCE = "/security/common-passwords.txt";

  private CommonPasswords() {}

  public static boolean contains(String password) {
    return Holder.PASSWORDS.contains(normalize(password));
  }

  static int size() {
    return Holder.PASSWORDS.size();
  }

  private static String normalize(String password) {
    return Normalizer.normalize(password, Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
  }

  /** Lazy holder: the list is read on first use, by whichever thread gets there first. */
  private static final class Holder {
    private static final Set<String> PASSWORDS = load();

    private static Set<String> load() {
      try (InputStream in = CommonPasswords.class.getResourceAsStream(RESOURCE)) {
        if (in == null) {
          throw new IllegalStateException("Missing password blocklist " + RESOURCE);
        }
        Set<String> passwords = new HashSet<>();
        try (BufferedReader reader =
            new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
          String line;
          while ((line = reader.readLine()) != null) {
            if (!line.isEmpty() && !line.startsWith("#")) {
              passwords.add(normalize(line));
            }
          }
        }
        return Set.copyOf(passwords);
      } catch (IOException e) {
        throw new UncheckedIOException("Could not read " + RESOURCE, e);
      }
    }
  }
}

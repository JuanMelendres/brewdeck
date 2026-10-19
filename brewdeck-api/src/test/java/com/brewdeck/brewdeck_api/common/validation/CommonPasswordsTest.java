package com.brewdeck.brewdeck_api.common.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CommonPasswordsTest {

  @Test
  void loadsTheBundledList() {
    assertThat(CommonPasswords.size()).isGreaterThan(5000);
  }

  @Test
  void matchesCommonPasswords_ignoringCase() {
    assertThat(CommonPasswords.contains("passwordpassword")).isTrue();
    assertThat(CommonPasswords.contains("PasswordPassword")).isTrue();
    assertThat(CommonPasswords.contains("qwertyuiopasdfghjkl")).isTrue();
    assertThat(CommonPasswords.contains("brewdeckpassword")).isTrue();
  }

  @Test
  void letsAnUnusualPassphraseThrough() {
    assertThat(CommonPasswords.contains("brew-test-pass-123")).isFalse();
    assertThat(CommonPasswords.contains("molienda fina y agua a 92,5 grados")).isFalse();
  }

  @Test
  void ignoresTheCommentHeader() {
    assertThat(
            CommonPasswords.contains(
                "# Common passwords of 15 to 128 characters, lowercased (ADR-018, AUTH-03)."))
        .isFalse();
  }
}

package com.brewdeck.brewdeck_api.account;

import com.brewdeck.brewdeck_api.auth.refresh.RefreshTokenCookies;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/me")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Registration, login, and current-user lookup")
public class AccountController {

  private final AccountService accountService;
  private final RefreshTokenCookies refreshTokenCookies;

  @GetMapping("/export")
  @Operation(
      summary = "Download all of the current user's data as JSON (ADR-020)",
      description = "Profile, coffees, own brew methods, recipes and brew sessions. 5 per hour.")
  public ResponseEntity<AccountExportResponse> export() {
    String filename = "brewdeck-export-" + LocalDate.now(ZoneOffset.UTC) + ".json";
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(filename).build().toString())
        .body(accountService.export());
  }

  @DeleteMapping
  @Operation(
      summary = "Delete the current account and all its data (ADR-020)",
      description =
          "Needs the current password. Cannot be undone. Ends every session and clears the"
              + " refresh cookie; a confirmation email goes to the deleted address.")
  public ResponseEntity<Void> delete(@Valid @RequestBody DeleteAccountRequest request) {
    accountService.delete(request);
    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, refreshTokenCookies.clear().toString())
        .build();
  }
}

package com.brewdeck.brewdeck_api.retention;

/**
 * What one retention purge run did (ADR-021).
 *
 * @param accountsChecked false when email verification is not enforced here, so accounts were left
 *     alone
 * @param failures accounts that hit an error; the rest of the run went on
 */
public record RetentionPurgeResult(
    boolean accountsChecked, int reminded, int deleted, int failures, long tokensDeleted) {}

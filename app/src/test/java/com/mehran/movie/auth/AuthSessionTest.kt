package com.mehran.movie.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The session must survive an idle period: the expiry stored with the tokens
 * drives the proactive refresh, and only a genuine auth rejection may wipe them.
 */
class AuthSessionTest {

    @Test
    fun durationStringsBecomeAbsoluteExpiry() {
        val now = 1_000_000L
        assertEquals(now + 300_000L, expiresAtFrom("5m", now))   // verified live value
        assertEquals(now + 3_600_000L, expiresAtFrom("1h", now))
        assertEquals(now + 30_000L, expiresAtFrom("30s", now))
        assertEquals(now + 86_400_000L, expiresAtFrom("1d", now))
    }

    @Test
    fun numericSecondsAreSupported() {
        val now = 1_000_000L
        assertEquals(now + 900_000L, expiresAtFrom("900", now))
        assertEquals(now + 900_000L, expiresAtFrom(900L.toString(), now))
    }

    @Test
    fun missingOrBrokenExpiryDisablesProactiveRefresh() {
        assertEquals(0L, expiresAtFrom(null))
        assertEquals(0L, expiresAtFrom(""))
        assertEquals(0L, expiresAtFrom("soon"))
        assertEquals(0L, expiresAtFrom("0m"))
        assertEquals(0L, expiresAtFrom("5x"))
    }

    @Test
    fun onlyAuthRejectionsClearTheSession() {
        // The refresh token itself was refused: sign in again.
        assertTrue(isTokenRejection(401, "{\"statusCode\":401,\"message\":\"Unauthorized\"}"))
        assertTrue(
            isTokenRejection(
                403,
                "{\"statusCode\":403,\"code\":\"INVALID_REFRESH_TOKEN\",\"message\":\"invalid refresh\"}"
            )
        )
        // Cloudflare / WAF / rate limits must not log the viewer out.
        assertFalse(isTokenRejection(403, "<!DOCTYPE html><html><body>error code: 1010</body></html>"))
        assertFalse(isTokenRejection(403, ""))
        assertFalse(isTokenRejection(429, "{\"message\":\"rate limited\"}"))
        assertFalse(isTokenRejection(500, "{\"message\":\"boom\"}"))
    }
}

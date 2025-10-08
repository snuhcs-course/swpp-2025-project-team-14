package com.example.mindlog.features.auth.data.api

import okhttp3.Authenticator
import okhttp3.Response
import okhttp3.Request
import okhttp3.Route
import kotlin.concurrent.Volatile
import com.example.mindlog.features.auth.util.TokenManager


class TokenAuthenticator(
    private val tokenManager: TokenManager,
    private val refreshApi: RefreshApi
) : Authenticator {

    @Volatile private var refreshing = false
    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.priorResponse() != null) return null
        val refresh = tokenManager.getRefreshToken() ?: return null

        synchronized(lock) {
            if (refreshing) return null
            refreshing = true
        }

        return try {
            val res = refreshApi.refresh(RefreshTokenRequest(refresh)).execute()
            if (!res.isSuccessful) return null
            val body = res.body() ?: return null
            val newAccess = body.data.access ?: return null
            val newRefresh = body.data.refresh ?: refresh

            tokenManager.saveTokens(newAccess, newRefresh)

            response.request().newBuilder()
                .header("Authorization", "Bearer $newAccess")
                .build()
        } catch (_: Exception) {
            null
        } finally {
            refreshing = false
        }
    }
}
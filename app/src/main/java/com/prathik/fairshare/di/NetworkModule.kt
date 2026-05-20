package com.prathik.fairshare.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.prathik.fairshare.BuildConfig
import com.prathik.fairshare.data.local.EncryptedTokenStore
import com.prathik.fairshare.data.network.api.AnalyticsApiService
import com.prathik.fairshare.data.network.api.AuthApiService
import com.prathik.fairshare.data.network.api.BalanceApiService
import com.prathik.fairshare.data.network.api.ExpenseApiService
import com.prathik.fairshare.data.network.api.FriendApiService
import com.prathik.fairshare.data.network.api.GroupApiService
import com.prathik.fairshare.data.network.api.ImportApiService
import com.prathik.fairshare.data.network.api.NotificationApiService
import com.prathik.fairshare.data.network.api.ReceiptApiService
import com.prathik.fairshare.data.network.api.ReminderApiService
import com.prathik.fairshare.data.network.api.SettlementApiService
import com.prathik.fairshare.data.network.api.UserApiService
import com.prathik.fairshare.data.network.interceptor.AuthInterceptor
import com.prathik.fairshare.data.network.interceptor.TokenRefreshInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenStore: EncryptedTokenStore): AuthInterceptor =
        AuthInterceptor(tokenStore)

    @Provides
    @Singleton
    fun provideTokenRefreshInterceptor(tokenStore: EncryptedTokenStore): TokenRefreshInterceptor =
        TokenRefreshInterceptor(tokenStore)

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenRefreshInterceptor: TokenRefreshInterceptor,
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        // Certificate pinning — prevents MITM attacks with rogue CA certs.
        // Pin = SHA-256 of the server's public key (survives cert renewal if same key pair).
        // Backup pin = Let's Encrypt R13 intermediate CA — ensures a new cert from the same
        // CA works during key rotation.
        // To regenerate the primary pin:
        //   echo | openssl s_client -connect api.fairshareapp.app:443 | openssl x509 -noout -pubkey |
        //   openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | base64
        //
        // IMPORTANT: BASE_URL = https://api.fairshareapp.app/ — pin must match this host,
        // not the root domain fairshareapp.app (which is only used for the web frontend).
        val certificatePinner = if (!BuildConfig.DEBUG) {
            okhttp3.CertificatePinner.Builder()
                // Leaf cert — regenerate when cert renews (Let's Encrypt ~90 days).
                .add("api.fairshareapp.app", "sha256/Ntak90V0psUlmdCwKUla6GZ1dgfSw9FHjiFYk/xhpYA=")
                // Let's Encrypt R13 intermediate CA — backup pin, stable across cert renewals.
                .add("api.fairshareapp.app", "sha256/AlSQhgtJirc8ahLyekmtX+Iw+v46yPYRLJt9Cq1GlB0=")
                .build()
        } else {
            null // Disable pinning in debug so Charles/Proxyman can be used
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(tokenRefreshInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)   // 2 min — import can take 48s+ for large CSVs
            .writeTimeout(60, TimeUnit.SECONDS)   // 1 min — large CSV body upload
            .apply { if (certificatePinner != null) certificatePinner(certificatePinner) }
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService =
        retrofit.create(AuthApiService::class.java)

    @Provides @Singleton
    fun provideUserApiService(retrofit: Retrofit): UserApiService =
        retrofit.create(UserApiService::class.java)

    @Provides @Singleton
    fun provideGroupApiService(retrofit: Retrofit): GroupApiService =
        retrofit.create(GroupApiService::class.java)

    @Provides @Singleton
    fun provideExpenseApiService(retrofit: Retrofit): ExpenseApiService =
        retrofit.create(ExpenseApiService::class.java)

    @Provides @Singleton
    fun provideBalanceApiService(retrofit: Retrofit): BalanceApiService =
        retrofit.create(BalanceApiService::class.java)

    @Provides @Singleton
    fun provideSettlementApiService(retrofit: Retrofit): SettlementApiService =
        retrofit.create(SettlementApiService::class.java)

    @Provides @Singleton
    fun provideFriendApiService(retrofit: Retrofit): FriendApiService =
        retrofit.create(FriendApiService::class.java)

    @Provides @Singleton
    fun provideNotificationApiService(retrofit: Retrofit): NotificationApiService =
        retrofit.create(NotificationApiService::class.java)

    @Provides @Singleton
    fun provideAnalyticsApiService(retrofit: Retrofit): AnalyticsApiService =
        retrofit.create(AnalyticsApiService::class.java)

    @Provides @Singleton
    fun provideReceiptApiService(retrofit: Retrofit): ReceiptApiService =
        retrofit.create(ReceiptApiService::class.java)

    @Provides @Singleton
    fun provideReminderApiService(retrofit: Retrofit): ReminderApiService =
        retrofit.create(ReminderApiService::class.java)

    @Provides @Singleton
    fun provideImportApiService(retrofit: Retrofit): ImportApiService =
        retrofit.create(ImportApiService::class.java)
}
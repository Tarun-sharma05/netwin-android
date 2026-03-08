package com.cehpoint.netwin.di

import com.cehpoint.netwin.data.repository.AdminConfigRepositoryImpl
import com.cehpoint.netwin.data.repository.AuthRepositoryImpl
import com.cehpoint.netwin.data.repository.MatchRepositoryImpl
import com.cehpoint.netwin.data.repository.TournamentRepositoryImpl
import com.cehpoint.netwin.data.repository.TransactionRepositoryImpl
import com.cehpoint.netwin.data.repository.KycRepositoryImpl
import com.cehpoint.netwin.data.repository.UserRepositoryImpl
import com.cehpoint.netwin.data.repository.WalletRepositoryImpl
import com.cehpoint.netwin.domain.repository.AdminConfigRepository
import com.cehpoint.netwin.domain.repository.AuthRepository
import com.cehpoint.netwin.domain.repository.MatchRepository
import com.cehpoint.netwin.domain.repository.TournamentRepository
import com.cehpoint.netwin.domain.repository.TransactionRepository
import com.cehpoint.netwin.domain.repository.KycRepository
import com.cehpoint.netwin.domain.repository.UserRepository
import com.cehpoint.netwin.domain.repository.WalletRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        impl: UserRepositoryImpl
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindTournamentRepository(
        impl: TournamentRepositoryImpl
    ): TournamentRepository

    @Binds
    @Singleton
    abstract fun bindMatchRepository(
        impl: MatchRepositoryImpl
    ): MatchRepository

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        impl: TransactionRepositoryImpl
    ): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindKycRepository(
        impl: KycRepositoryImpl
    ): KycRepository

    @Binds
    @Singleton
    abstract fun bindWalletRepository(
        impl: WalletRepositoryImpl
    ): WalletRepository

    @Binds
    @Singleton
    abstract fun bindAdminConfigRepository(
        impl: AdminConfigRepositoryImpl
    ): AdminConfigRepository
} 
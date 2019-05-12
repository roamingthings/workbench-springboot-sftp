package de.roamingthings.workbench.sftp.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.integration.jdbc.lock.DefaultLockRepository
import org.springframework.integration.jdbc.lock.JdbcLockRegistry
import org.springframework.integration.jdbc.lock.LockRepository
import org.springframework.integration.support.leader.LockRegistryLeaderInitiator
import org.springframework.integration.support.locks.LockRegistry
import javax.sql.DataSource

@Configuration
class IntegrationLockingConfiguration {

    @Bean
    fun lockRepository(dataSource: DataSource): DefaultLockRepository {
        return DefaultLockRepository(dataSource)
    }

    @Bean
    fun lockRegistry(lockRepository: LockRepository): JdbcLockRegistry {
        return JdbcLockRegistry(lockRepository)
    }

    @Bean
    fun leaderInitiator(lockRegistry: LockRegistry): LockRegistryLeaderInitiator {
        return LockRegistryLeaderInitiator(lockRegistry)
    }
}

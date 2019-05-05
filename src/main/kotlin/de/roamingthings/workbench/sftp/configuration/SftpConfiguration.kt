package de.roamingthings.workbench.sftp.configuration

import org.springframework.boot.context.properties.ConfigurationProperties


@ConfigurationProperties(prefix = "sftp")
class SftpConfiguration {

    lateinit var keysFolder: String

    lateinit var strictHostKeyChecking: String

    lateinit var userPrivateKey: String

    lateinit var userPrivateKeyPassphrase: String

    lateinit var username: String

    lateinit var remoteHost: String

    var remotePort: Int = 0
}

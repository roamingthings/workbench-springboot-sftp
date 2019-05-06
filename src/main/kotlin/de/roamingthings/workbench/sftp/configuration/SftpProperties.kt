package de.roamingthings.workbench.sftp.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.core.io.Resource


@ConfigurationProperties(prefix = "sftp")
class SftpProperties {

    lateinit var host: String

    var port: Int = 0

    lateinit var user: String

    lateinit var strictHostKeyChecking: String

    var knownHosts: Resource? = null

    var userPrivateKey: Resource? = null

    var userPublicKey: Resource? = null

    lateinit var userPrivateKeyPassphrase: String

    var password: String? = null

    lateinit var remoteDirectory: String
}

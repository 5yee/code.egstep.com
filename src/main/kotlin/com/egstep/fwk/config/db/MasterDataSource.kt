package com.egstep.fwk.config.db

import ch.qos.logback.classic.Logger
import com.zaxxer.hikari.HikariDataSource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import org.springframework.context.annotation.Profile
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.data.transaction.ChainedTransactionManager
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.Database
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import org.springframework.transaction.PlatformTransactionManager
import javax.persistence.EntityManagerFactory
import javax.sql.DataSource

@Configuration
@EnableJpaRepositories(basePackages = ["com.egstep.code.repo.jpa"],
    entityManagerFactoryRef = "entityManagerFactory",
    transactionManagerRef = "primaryTransactionManager")
class MasterDataSource {
    companion object {
        private val log = LoggerFactory.getLogger(MasterDataSource::class.java) as Logger
    }

    @Bean(name = ["dsMaster"])
    fun dsMaster(
        @Value("\${db.jpa.master.url}") url: String,
        @Value("\${db.common.minIdle}") minIdle: Int,
        @Value("\${db.common.maxPoolSize}") maxPoolSize: Int,
        @Value("\${db.common.idleTimeout}") idleTimeout: Long,
        @Value("\${db.master.userName}") userName: String,
        @Value("\${db.master.password}") password: String
    ): DataSource {
        log.info("=============== JPA Primary DataSource Setting Start =============== ")

        val ds = HikariDataSource()
        ds.jdbcUrl = url
        ds.username = userName
        ds.password = password
        ds.minimumIdle = 5
        ds.maximumPoolSize = 100
        ds.idleTimeout = 3000
        ds.connectionInitSql = "set time zone 'Asia/Seoul'"

        log.info("=============== JPA Public DataSource Setting End   =============== ")

        return ds
    }

    @Bean(name = ["entityManagerFactory"])
    @Order(Ordered.LOWEST_PRECEDENCE)
    fun entityManagerFactory(
        @Qualifier("dsMaster") dataSource: DataSource,
        @Value("\${db.common.dialect}") dialect: String,
        @Value("\${db.jpa.master.schema}") schema: String,
        @Value("\${db.jpa.master.ddl-auto}") ddlAuto: String,
        @Value("\${db.jpa.master.entity-packages}") entityPackages: Array<String>
    ): LocalContainerEntityManagerFactoryBean {

        val vendorAdapter = HibernateJpaVendorAdapter()
        vendorAdapter.setDatabase(Database.POSTGRESQL)
        vendorAdapter.setGenerateDdl(true)

        val properties: HashMap<String, Any> = hashMapOf()
        properties["hibernate.default_schema"] = schema
        properties["hibernate.hbm2ddl.auto"] = ddlAuto
        properties["hibernate.ddl-auto"] = ddlAuto
        properties["hibernate.dialect"] = dialect
        properties["hibernate.physical_naming_strategy"] = "org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy"
        properties["hibernate.cache.use_second_level_cache"] = false
        properties["hibernate.cache.use_query_cache"] = false
        properties["hibernate.show_sql"] = false
        properties["javax.persistence.validation.mode"] = "none"

        val em = LocalContainerEntityManagerFactoryBean()
        em.dataSource = dataSource
        em.jpaVendorAdapter = vendorAdapter
        em.setPackagesToScan(*entityPackages)
        em.setJpaPropertyMap(properties)

        return em
    }

    @Bean(name=["primaryTransactionManager"])
    @Order(Ordered.LOWEST_PRECEDENCE)
    fun transactionManager(@Qualifier("entityManagerFactory") entityManagerFactory: EntityManagerFactory,
                           @Qualifier("dsMaster") dataSource: DataSource
    ): PlatformTransactionManager {
        val jtm = JpaTransactionManager(entityManagerFactory)
        val dstm = DataSourceTransactionManager()
        dstm.dataSource = dataSource

        val ctm = ChainedTransactionManager(jtm, dstm)
        return ctm
    }


}

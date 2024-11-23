package com.aleksandr_kobelskiy.week12practice.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//@Configuration
//@EnableConfigurationProperties({ R2dbcProperties.class, FlywayProperties.class })
//public class DatabaseConfig {
//    @Bean(initMethod = "migrate")
//    public Flyway flyway(FlywayProperties flywayProperties, R2dbcProperties r2dbcProperties) {
//        return Flyway.configure()
//                .dataSource(
//                        flywayProperties.getUrl(),
////                        r2dbcProperties.getUsername(),
////                        r2dbcProperties.getPassword()
//                        flywayProperties.getUser(),
//                        flywayProperties.getPassword()
//                )
//                .locations(flywayProperties.getLocations()
//                        .stream()
//                        .toArray(String[]::new))
//                .baselineOnMigrate(true)
//                .load();
//    }
//}

@Configuration
@EnableConfigurationProperties({ FlywayProperties.class })
public class FlywayMigrationConfig {
    @Bean(initMethod = "migrate")
    public Flyway flyway(FlywayProperties flywayProperties) {
        return Flyway.configure()
                .dataSource(
                        flywayProperties.getUrl(),
                        flywayProperties.getUser(),
                        flywayProperties.getPassword()
                )
                .locations(flywayProperties.getLocations()
                        .stream()
                        .toArray(String[]::new))
                .baselineOnMigrate(true)
                .load();
    }
}
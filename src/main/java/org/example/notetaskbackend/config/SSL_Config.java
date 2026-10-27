package org.example.notetaskbackend.config;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import javax.net.ssl.TrustManagerFactory;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;

@Configuration
public class SSL_Config {

    @Bean
    public SslContext gigaChatSslContext(
            @Value("classpath:certs/root_ca.crt") Resource rootCa,
            @Value("classpath:certs/sub_ca.crt") Resource subCa,
            @Value("classpath:certs/server_cert.crt") Resource serverCert
    ) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        keyStore.setCertificateEntry("root_ca", cf.generateCertificate(rootCa.getInputStream()));
        keyStore.setCertificateEntry("sub_ca", cf.generateCertificate(subCa.getInputStream()));
        keyStore.setCertificateEntry("server_cert", cf.generateCertificate(serverCert.getInputStream()));

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);

        return SslContextBuilder.forClient()
                .trustManager(tmf)
                .build();
    }
}
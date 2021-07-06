package it.eichert.camel.routes;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import it.eichert.camel.configuration.GenericRouteConfiguration;
import it.eichert.camel.models.MetaRemoteFile;
import it.eichert.camel.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
public class FileRoute extends RouteBuilder implements ApplicationContextAware {
    private ApplicationContext applicationContext;

    private GenericRouteConfiguration configuration;

    private final Cache<String, MetaRemoteFile> cache = Caffeine.newBuilder().build();

    private static final String CAMEL_FILE_ABSOLUTE_PATH = "CamelFileAbsolutePath";
    private static final String CAMEL_FILE_PARENT = "CamelFileParent";
    private static final String CUSTOM_META_REMOTE_FILE_ID = "CustomMetaRemoteFileId";
    private static final String CUSTOM_META_REMOTE_FILE_PATH = "CustomMetaRemoteFilePath";
    private static final String CUSTOM_META_REMOTE_FILE_NAME = "CustomMetaRemoteFileName";

    @PostConstruct
    private void initConfig() {
        configuration = Utils.bindProperties("integration.file-integration", GenericRouteConfiguration.class, applicationContext);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void configure() throws Exception {
        from(configuration.getFrom().getUri())
                .routeId("watch data route")
                .log("indexing ${header." + Exchange.FILE_NAME + "}")
                .process(exchange -> {
                    final String parentPath = exchange.getIn().getHeader(CAMEL_FILE_PARENT, String.class);
                    final String filename = exchange.getIn().getHeader(Exchange.FILE_NAME_ONLY, String.class);
                    final MetaRemoteFile metaRemoteFile = new MetaRemoteFile(filename, parentPath);
                    cache.put(metaRemoteFile.getUuid().toString(), metaRemoteFile);

                });

        restConfiguration().component("servlet").bindingMode(RestBindingMode.auto);

        from("rest:get:data")
                .routeId("list data route")
                .log("list data")
                .setBody(constant(cache.asMap().values()))
                .marshal()
                .json(JsonLibrary.Jackson);


        from("rest:get:data:/{CustomMetaRemoteFileId}")
                .routeId("get data route")
                .log("get data by id ${header.CustomMetaRemoteFileId}")
                .process(exchange -> {
                    /*set response*/
                    final String uuid = exchange.getIn().getHeader(CUSTOM_META_REMOTE_FILE_ID, String.class);
                    final MetaRemoteFile metaRemoteFile = cache.getIfPresent(uuid);
                    if (metaRemoteFile != null) {
                        exchange.getIn().setHeader(CUSTOM_META_REMOTE_FILE_PATH, metaRemoteFile.getParentPath());
                        exchange.getIn().setHeader(CUSTOM_META_REMOTE_FILE_NAME, metaRemoteFile.getFilename());
                    }
                })
                .filter(header(CUSTOM_META_REMOTE_FILE_PATH).isNotNull())
                .log("fetching ${header." + CUSTOM_META_REMOTE_FILE_PATH + "} from sftp")
                .pollEnrich()
                .simple("sftp:localhost:22/${header." + CUSTOM_META_REMOTE_FILE_PATH + "}?username=tester&password=password&noop=true&idempotent=false&fileName=${header." + CUSTOM_META_REMOTE_FILE_NAME + "}")
                .end();
    }


}

package org.backmeup.keyserver.rest;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.dozer.DozerBeanMapper;
import org.dozer.Mapper;

@ApplicationScoped
public class MapperProducer {
    private static final String DOZER_APP_MAPPING = "dozer-App-mapping.xml";
    private static final String DOZER_AUTHRESPONSE_MAPPING = "dozer-AuthResponse-mapping.xml";
    private static final String DOZER_TOKEN_MAPPING = "dozer-Token-mapping.xml";

    private Mapper mapper;

    @Produces
    public Mapper getMapper() {
        if (mapper == null) {
            List<String> configList = new ArrayList<>();
            configList.add(DOZER_APP_MAPPING);
            configList.add(DOZER_AUTHRESPONSE_MAPPING);
            configList.add(DOZER_TOKEN_MAPPING);
            mapper = new DozerBeanMapper(configList);
        }
        return mapper;
    }
}

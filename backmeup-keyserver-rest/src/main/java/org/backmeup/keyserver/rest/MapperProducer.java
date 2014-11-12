package org.backmeup.keyserver.rest;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.dozer.DozerBeanMapper;
import org.dozer.Mapper;

@ApplicationScoped
public class MapperProducer {
    private static final String DOZER_CUSTOM_CONVERTERS = "dozer-custom-converters.xml";
    private static final String DOZER_USER_MAPPING = "dozer-user-mapping.xml";
    private static final String DOZER_PROFILE_MAPPING = "dozer-profile-mapping.xml";
    private static final String DOZER_BACKUPJOB_MAPPING = "dozer-backupjob-mapping.xml";
    private static final String DOZER_SEARCH_MAPPING = "dozer-search-mapping.xml";

    private Mapper mapper;

    @Produces
    public Mapper getMapper() {
        if (mapper == null) {
            List<String> configList = new ArrayList<>();
            configList.add(DOZER_CUSTOM_CONVERTERS);
            configList.add(DOZER_USER_MAPPING);
            configList.add(DOZER_PROFILE_MAPPING);
            configList.add(DOZER_BACKUPJOB_MAPPING);
            configList.add(DOZER_SEARCH_MAPPING);
            mapper = new DozerBeanMapper(configList);
        }
        return mapper;
    }
}

package gsrs.module.substance.autoconfigure;

import gsrs.EnableGsrsAkka;
import gsrs.EnableGsrsApi;
import gsrs.EnableGsrsJpaEntities;
import gsrs.GsrsFactoryConfiguration;
import gsrs.cache.GsrsCache;
import gsrs.controller.EditEntityService;
import gsrs.module.substance.SubstanceCoreConfiguration;
import gsrs.module.substance.SubstanceEntityService;
import gsrs.module.substance.SubstanceEntityServiceImpl;
import gsrs.module.substance.controllers.*;
import gsrs.module.substance.repository.NucleicAcidSubstanceRepository;
import gsrs.module.substance.repository.ProteinSubstanceRepository;
import gsrs.module.substance.repository.SubunitRepository;
import gsrs.module.substance.services.*;
import gsrs.service.PayloadService;
import ix.seqaln.service.SequenceIndexerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.util.StreamUtils;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@AutoConfiguration
@EnableConfigurationProperties
@EnableGsrsAkka
@EnableGsrsJpaEntities
@EnableGsrsApi
@Import({SubstanceCoreConfiguration.class,
        MolwitchLoader.class
})
public class GsrsSubstanceModuleAutoConfiguration {

    @Autowired
    private SequenceIndexerService sequenceIndexerService;


    @Autowired
    private SubstanceStructureSearchService structureSearchService;


    @Autowired
    private GsrsCache ixCache;

    @Autowired
    PayloadService payloadService;
    @Autowired
    private ProteinSubstanceRepository proteinSubstanceRepository;
    @Autowired
    private NucleicAcidSubstanceRepository nucleicAcidSubstanceRepository;
    @Autowired
    private SubunitRepository subunitRepository;



    /**
     * This is to allow async events
     * @return
     */
//    @Bean(name = "applicationEventMulticaster")
//    public ApplicationEventMulticaster simpleApplicationEventMulticaster() {
//        SimpleApplicationEventMulticaster eventMulticaster =
//                new SimpleApplicationEventMulticaster();
//
//        eventMulticaster.setTaskExecutor(new SimpleAsyncTaskExecutor());
//        return eventMulticaster;
//    }

    @Primary
    @Bean
    @ConfigurationProperties("gsrs")
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public GsrsFactoryConfiguration gsrsFactoryConfiguration(){
        return new GsrsFactoryConfiguration();
    }

    @Bean("SubstanceSequenceSearchService")
    @ConditionalOnMissingBean(SubstanceSequenceSearchService.class)
    public SubstanceSequenceSearchService getSequenceSearchService(){
        return new LegacySubstanceSequenceSearchService(sequenceIndexerService, ixCache,
                proteinSubstanceRepository, nucleicAcidSubstanceRepository, subunitRepository);
    }

    @Bean("SubstanceStructureSearchService")
    @ConditionalOnMissingBean(SubstanceStructureSearchService.class)
    public SubstanceStructureSearchService getStructureSearchService(){
        return new SubstanceStructureSearchService();
    }
    @Bean
    @ConditionalOnMissingBean(StructureSearchConfiguration.class)
    public StructureSearchConfiguration getStructureSearchConfiguration(){
        return new StructureSearchConfiguration();
    }
    //let's try creating the beans for our controllers and services

    @Bean
    @ConditionalOnMissingBean(RecalcStructurePropertiesService.class)
    public RecalcStructurePropertiesService recalcStructurePropertiesService(){
        return new RecalcStructurePropertiesService();
    }
    @Bean
    @ConditionalOnMissingBean(SubstanceEntityService.class)
    public SubstanceEntityService substanceEntityService(){
        return new SubstanceEntityServiceImpl();
    }
    @Bean
    @ConditionalOnMissingBean(LegacyGinasAppController.class)
    public LegacyGinasAppController legacyGinasAppController(){
        return new LegacyGinasAppController();
    }
    @Bean
    @ConditionalOnMissingBean(EditEntityService.class)
    public EditEntityService editEntityService(){
        return new EditEntityService();
    }
    @Bean
    @ConditionalOnMissingBean(SubstanceController.class)
    public SubstanceController substanceController(){
        return new SubstanceController();
    }
    @Bean
    @ConditionalOnMissingBean(StructureOCRController.class)
    public StructureOCRController ocrController(){
        return new StructureOCRController();
    }

    @Bean
    @ConditionalOnMissingBean(ReIndexController.class)
    public ReIndexController reIndexController(){
        return new ReIndexController();
    }

    @Bean
    @ConditionalOnMissingBean(ReindexService.class)
    public ReindexService reindexService(){
        return new ReindexFromBackups();
    }

    @Bean
    @ConditionalOnMissingBean(ReindexStatusEventListener.class)
    public ReindexStatusEventListener reindexStatusEventListener(){
        return new ReindexStatusEventListener();
    }

    @Bean
    @ConditionalOnMissingBean(NameEntityService.class)
    public NameEntityService nameEntityService(){
        return new NameEntityService();
    }
    @Bean
    @ConditionalOnMissingBean(CodeEntityService.class)
    public CodeEntityService codeEntityService(){
        return new CodeEntityService();
    }
    @Bean
    @ConditionalOnMissingBean(ReferenceEntityService.class)
    public ReferenceEntityService referenceEntityService(){
        return new ReferenceEntityService();
    }

    @Bean
    @ConditionalOnMissingBean(RelationshipService.class)
    public RelationshipService relationshipService(){
        return new RelationshipService();
    }

    @Bean
    @ConditionalOnMissingBean(EntityManagerSubstanceKeyResolver.class)
    public EntityManagerSubstanceKeyResolver entityManagerSubstanceKeyResolverService(){
        return new EntityManagerSubstanceKeyResolver();
    }

    @Bean
    @ConditionalOnMissingBean(name = "gsrsJsonStringRequestBodyWebMvcConfigurer")
    public WebMvcConfigurer gsrsJsonStringRequestBodyWebMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void configureMessageConverters(HttpMessageConverters.ServerBuilder builder) {
                builder.configureMessageConvertersList(converters ->
                        converters.add(0, new JsonStringRequestBodyHttpMessageConverter()));
            }
        };
    }

    static final class JsonStringRequestBodyHttpMessageConverter extends AbstractHttpMessageConverter<String> {
        private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
        private static final MediaType APPLICATION_JSON_SUFFIX = MediaType.parseMediaType("application/*+json");

        JsonStringRequestBodyHttpMessageConverter() {
            super(DEFAULT_CHARSET, MediaType.APPLICATION_JSON, APPLICATION_JSON_SUFFIX);
        }

        @Override
        public boolean canWrite(Class<?> clazz, MediaType mediaType) {
            return false;
        }

        @Override
        protected boolean supports(Class<?> clazz) {
            return String.class == clazz;
        }

        @Override
        protected String readInternal(Class<? extends String> clazz, HttpInputMessage inputMessage) throws IOException {
            MediaType contentType = inputMessage.getHeaders().getContentType();
            Charset charset = contentType != null && contentType.getCharset() != null
                    ? contentType.getCharset()
                    : DEFAULT_CHARSET;
            return StreamUtils.copyToString(inputMessage.getBody(), charset);
        }

        @Override
        protected void writeInternal(String value, HttpOutputMessage outputMessage) {
            throw new UnsupportedOperationException("This converter only supports request-body reads.");
        }
    }
}

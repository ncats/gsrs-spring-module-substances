package gsrs.module.substance.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import gsrs.repository.UserProfileRepository;
import gsrs.security.AdminService;
import ix.ginas.models.v1.Substance;
import jdk.internal.loader.Loader;
import lombok.SneakyThrows;
import org.springframework.transaction.support.TransactionTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.controller.GsrsControllerConfiguration;
import gsrs.module.substance.SubstanceEntityService;
import gsrs.module.substance.services.ProcessingJobEntityService;
import gsrs.module.substance.services.SubstanceBulkLoadService;
import gsrs.payload.PayloadController;
import gsrs.repository.PayloadRepository;
import gsrs.security.hasAdminRole;
import gsrs.service.GsrsEntityService;
import gsrs.service.PayloadService;
import ix.core.models.Payload;
import ix.core.models.ProcessingJob;
import ix.core.models.UserProfile;
import ix.core.processing.PayloadProcessor;
import ix.core.processing.TransformedRecord;
import ix.core.stats.Statistics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;


@RestController
@Slf4j
public class SubstanceEasyLegacyBulkLoadController {
    @Autowired
    private AdminService adminService;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private SubstanceEntityService substanceEntityService;

    @Autowired
    private PayloadService payloadService;

    @Autowired
    private PayloadRepository payloadRepository;

    @Autowired
    private SubstanceBulkLoadService substanceBulkLoadService;

    @Autowired
    private GsrsControllerConfiguration controllerConfiguration;

    @Autowired
    private ProcessingJobEntityService processingJobService;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;
    SubstanceEasyLegacyBulkLoadController(){

        System.out.println("Inside controller ... SubstanceEasyLegacyBulkLoadController.");
    }
    @hasAdminRole
    @GetMapping("/api/v1/testLoad")
    public ResponseEntity<String> testLoad(){
        String resultString = "Hello";
        Resource dataFile = new ClassPathResource(
        "rep18.gsrs"
        );

        TransactionTemplate tx = new TransactionTemplate(platformTransactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        Payload payload = tx.execute(status-> {
//            createUser("admin2", Role.values());
            //the old json has user TYLER and FDA-SRS too
//            createUser("TYLER", Role.values());
//            createUser("FDA_SRS", Role.values());
            try (InputStream in = dataFile.getInputStream()) {
                return payloadService.createPayload(dataFile.getFilename(), "ignore",
                in, PayloadService.PayloadPersistType.PERM);
            }catch(IOException e){
                throw new UncheckedIOException(e);
            }
        });
        PayloadProcessor pp = substanceBulkLoadService.submit(SubstanceBulkLoadService.SubstanceBulkLoadParameters.builder()
        .payload(payload)
        .build());

        String statKey = pp.key;
        boolean done =false;
        Statistics statistics=null;
        while(!done){
            statistics = substanceBulkLoadService.getStatisticsFor(statKey);

            if(statistics._isDone()){
                System.out.println(statistics);
                break;
            }
            // Thread.sleep(1000);
        }
        return ResponseEntity.ok(resultString);

    }
    @hasAdminRole
    @GetMapping("/api/v1/testLoad3")
    public ResponseEntity<String> testLoad3(){
        String resultString = "hello 3";
        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
        Resource dataFile = new ClassPathResource(
        "rep18.gsrs"
        );
        File f = null;
        try {
            f = dataFile.getFile();
        } catch (IOException e) {
            f = null;
            e.printStackTrace();
        }

        // String pathToLoadFile = System.getProperty("ix.ginas.load.file");
        //&& substanceRepository.count()==0
        if (f.getAbsolutePath() != null ) {
            // File f = new File(pathToLoadFile);
            if(f.exists()) {
                transactionTemplate.executeWithoutResult(status-> {
                    UserProfile up = userProfileRepository.findByUser_UsernameIgnoreCase("admin").standardize();

                    Authentication auth = new UsernamePasswordAuthenticationToken(up.user.username, null,
                    up.getRoles().stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r.name())).collect(Collectors.toList()));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                });
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(f.getAbsolutePath()))))) {
                    String line;
                    Pattern sep = Pattern.compile("\t");
                    ObjectMapper mapper = new ObjectMapper();
                    int i = 0;
                    long start = System.currentTimeMillis();
                    while ((line = reader.readLine()) != null) {

                        String[] cols = sep.split(line);
//                System.out.println(cols[2]);
                        JsonNode json = mapper.readTree(cols[2]);
                        try {
                            transactionTemplate.executeWithoutResult(status-> {
                                try {
                                    Substance s = substanceEntityService.createEntity(json, true).getCreatedEntity();
                                    if(s != null) {
                                        String uuid = s.uuid.toString();
                                        uuid = (uuid==null) ? "UUID_NULL": uuid;
                                        System.out.println("Loaded: " + uuid);
                                    } else {
                                        System.out.println("Loaded: " + "SUBSTANCE_NULL");
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    status.setRollbackOnly();
                                }
                            });
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }





                        i++;
                        if (i % 100 == 0) {
                            System.out.println("loaded record " + i);
                        }

                    }
                    System.out.println("done loading file");
                    long finish = System.currentTimeMillis();
                    long millisElapsed = finish - start;
                    long minutesElapsed = millisElapsed / (60 * 1000);
                    System.out.println("# Loaded: " + i);
                    System.out.println("Millis elapsed: " + millisElapsed);
                    System.out.println("Minutes elapsed: " + minutesElapsed);

                }catch(Throwable t){
                    t.printStackTrace();
                }
            }else{
                System.err.println("could not find GSRS file: " + f.getAbsolutePath());
            }
        }

        return ResponseEntity.ok(resultString);
    }

    @hasAdminRole
    @GetMapping("/api/v1/testLoad4")
    public ResponseEntity<String> testLoad4(){
        String resultString = "hello 4";
        final TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        Resource dataFile = new ClassPathResource("rep18.gsrs");
        Authentication auth = adminService.getCurrentAdminAuth();
        File f = null;
        try {
            f = dataFile.getFile();
        } catch (IOException e) {
            f = null;
            e.printStackTrace();
        }
        if (f.getAbsolutePath() != null ) {
            if(f.exists()) {
//                int parallelism = Runtime.getRuntime().availableProcessors();
                int parallelism = 4;

                List<Future> tasks = new ArrayList<>();
                ExecutorService executorService = Executors.newFixedThreadPool(parallelism);
//                Authentication auth = SecurityContextHolder.getContext().getAuthentication();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(f.getAbsolutePath()))))) {
                    reader.lines().forEach(l -> {
                        // processItem(l,transactionTemplate);

                        Future task = executorService.submit(() -> {
                            Runnable r = ()->{
                                _countrun.incrementAndGet();
                                processItem(l,transactionTemplate);
                            };
                            adminService.runAs(auth, r);

                        });
                        tasks.add(task);
                    });

                    for (Future task : tasks) {
                        try {
                            task.get();


                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }
                    }
                    System.out.println("_countrun:" + _countrun);
                    System.out.println("_countpi:" + _countpi);

               } catch (IOException e) {
                    System.out.println("IO Exception during buffered read ...");
                }

            }else{
                System.err.println("could not find GSRS file: " + f.getAbsolutePath());
            }

        }

        return ResponseEntity.ok(resultString);
    }

    class MyRunnable implements Runnable {

        String line;
        TransactionTemplate transactionTemplate;
        AdminService adminService;
        Authentication auth;
        SubstanceEasyLegacyBulkLoadController parent;

        public MyRunnable(
            String line,
            TransactionTemplate transactionTemplate,
            AdminService adminService,
            Authentication auth,
            SubstanceEasyLegacyBulkLoadController parent
        ) {}

        @Override
        public void run() {
            Runnable r = ()->{
                parent.processItem(line,transactionTemplate);
            };
            adminService.runAs(auth, r);
        }
    }

    private final String SEP = "\t";
    private final  ObjectMapper MAPPER = new ObjectMapper();
    private final AtomicInteger _countrun = new AtomicInteger(0);
    private final AtomicInteger _countpi = new AtomicInteger(0);


    private  void processItem(String line, TransactionTemplate transactionTemplate) {

        String[] cols = line.split(SEP);
        try {
            transactionTemplate.executeWithoutResult(status -> {
                _countpi.incrementAndGet();
                try {
                    JsonNode json = MAPPER.readTree(cols[2]);
                    Substance s = substanceEntityService.createEntity(json, true).getCreatedEntity();
                    if (s != null) {
                        String uuid = s.uuid.toString();
                        uuid = (uuid == null) ? "UUID_NULL" : uuid;
                        System.out.println("Loaded: " + uuid);
                    } else {
                        System.out.println("Loaded: " + "SUBSTANCE_NULL");
                    }
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                    status.setRollbackOnly();
                }
            });
        } catch (Throwable t) {
            System.out.println("THROWABLE: " + t.getMessage());
        }
    }

//    class LoaderTask implements Future {
//        String line;
//        Authentication auth;
//        ExecutorService executorService;
//
//        LoaderTask(String line, Authentication auth, ExecutorService executorService) {
//             this.line = line;
//             this.auth = auth;
//            Future task = executorService.submit(() -> {
//                Runnable r = ()->{
//                    processItem(l,transactionTemplate);
//                };
//                adminService.runAs(auth, r);
//            });
//
//        }
//
//
//        @Override
//        public boolean cancel(boolean mayInterruptIfRunning) {
//            return false;
//        }
//
//        @Override
//        public boolean isCancelled() {
//            return false;
//        }
//
//        @Override
//        public boolean isDone() {
//            return false;
//        }
//
//        @Override
//        public Object get() throws InterruptedException, ExecutionException {
//            return null;
//        }
//
//        @Override
//        public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
//            return null;
//        }
//    }

}



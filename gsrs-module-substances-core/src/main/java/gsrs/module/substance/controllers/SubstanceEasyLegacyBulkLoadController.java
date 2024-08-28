package gsrs.module.substance.controllers;

// import gsrs.repository.UserProfileRepository;
import gov.nih.ncats.common.executors.BlockingSubmitExecutor;
import gsrs.security.AdminService;
import ix.ginas.models.v1.Substance;
import org.springframework.transaction.support.TransactionTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.controller.GsrsControllerConfiguration;
import gsrs.module.substance.SubstanceEntityService;
import gsrs.module.substance.services.ProcessingJobEntityService;
import gsrs.module.substance.services.SubstanceBulkLoadService;
import gsrs.repository.PayloadRepository;
import gsrs.security.hasAdminRole;
import gsrs.service.PayloadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;


@RestController
@Slf4j
public class SubstanceEasyLegacyBulkLoadController {
    @Autowired
    private AdminService adminService;

//     @Autowired
//    private UserProfileRepository userProfileRepository;

    @Autowired
    private SubstanceEntityService substanceEntityService;

    @Autowired
    private PayloadService payloadService;

//    @Autowired
//    private PayloadRepository payloadRepository;

//     @Autowired
//    private SubstanceBulkLoadService substanceBulkLoadService;

//    @Autowired
//    private GsrsControllerConfiguration controllerConfiguration;

//    @Autowired
//    private ProcessingJobEntityService processingJobService;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

//    private  Authentication auth;

    SubstanceEasyLegacyBulkLoadController(
        // AdminService adminService
    ){
        this.adminService = adminService;
        // Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // Authentication auth = adminService.getAnyAdmin();
    }

    @hasAdminRole
    @GetMapping("/api/v1/testLoad4")
    public ResponseEntity<String> testLoad4(){
        String resultString = "hello 4";
        final TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        Resource dataFile = new ClassPathResource("rep18.gsrs");

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
                int parallelism = 2;

                List<Future> tasks = new ArrayList<>();
           //      ExecutorService executorService = Executors.newFixedThreadPool(parallelism);
                ExecutorService executorService = BlockingSubmitExecutor.newFixedThreadPool(parallelism, 5);


//                Authentication auth = SecurityContextHolder.getContext().getAuthentication();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(f.getAbsolutePath()))))) {
                    reader.lines().forEach(l -> {
                        Authentication auth = adminService.getAnyAdmin();
                        Future task = executorService.submit(() -> {
                            adminService.runAs(auth,
                                (Runnable) ()->{
                                    System.out.println("AUTH: "  + auth.toString());
                                    _countrun.incrementAndGet();


                                    //  processItem(l, auth, transactionTemplate);

                                    long threadId = Thread.currentThread().getId();
                                    System.out.println("Thread # " + threadId + " is doing this task");
                                    String[] cols = l.split(SEP);
                                    try {
                                        transactionTemplate.executeWithoutResult(status -> {
                                            _countpi.incrementAndGet();
                                            try {
                                                JsonNode json = MAPPER.readTree(cols[2]);
                                                // Substance s =
                                                substanceEntityService.createEntity(json, true); //.getCreatedEntity();
//                                                if (s != null) {
//                                                    String uuid = s.uuid.toString();
//                                                    uuid = (uuid == null) ? "UUID_NULL" : uuid;
//                                                    System.out.println("Loaded: " + uuid);
//                                                } else {
//                                                    System.out.println("Loaded: " + "SUBSTANCE_NULL");
//                                                }
                                            } catch (IOException e) {
                                                System.out.println(e.getMessage());
                                                status.setRollbackOnly();
                                            }
                                        });
                                    } catch (Throwable t) {
                                        long threadId2 = Thread.currentThread().getId();
                                        System.out.println("THROWABLE -- " + "Thread # " + threadId  + " ... " + t.getMessage());
                                    }
                                    System.out.println("Auth Service");
                                    System.out.println(auth.toString());

                           });
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


    private final String SEP = "\t";
    private final  ObjectMapper MAPPER = new ObjectMapper();
    private final AtomicInteger _countrun = new AtomicInteger(0);
    private final AtomicInteger _countpi = new AtomicInteger(0);


    private  void processItem(String line, Authentication auth, TransactionTemplate transactionTemplate) {
        System.out.println("AUTH:" + auth.getName());
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

}



package example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.module.substance.SubstanceEntityService;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.repository.GroupRepository;
import gsrs.repository.UserProfileRepository;
import ix.core.models.Group;
import ix.core.models.Principal;
import ix.core.models.Role;
import ix.core.models.UserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.*;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

@Profile("!test")
@Component
@Order
public class LoadGroupsAndUsersOnStartup implements ApplicationRunner {



//    @Autowired
//    GsrsValidatorFactory validationFactory;
    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private SubstanceRepository substanceRepository;


    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Autowired
    private SubstanceEntityService substanceEntityService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
        transactionTemplate.executeWithoutResult(status->{
            if(groupRepository.count() ==0) {


                groupRepository.save(new Group("protected"));
                groupRepository.save(new Group("admin"));


                UserProfile up = new UserProfile();
                up.user = new Principal("admin", "admin@example.com");
                up.setPassword("admin");
                up.active = true;
                up.user.admin = true;
                up.deprecated = false;
                up.setRoles(Arrays.asList(Role.values()));

                userProfileRepository.saveAndFlush(up);

                UserProfile up2 = new UserProfile();
                up2.user = new Principal("user1", "user1@example.com");
                up2.setPassword("user1");
                up2.active = true;
                up2.deprecated = false;
                up2.setRoles(Arrays.asList(Role.Query));

                userProfileRepository.saveAndFlush(up2);

                UserProfile guest = new UserProfile();
                guest.user = new Principal("GUEST", null);
                guest.setPassword("GUEST");
                guest.active = false;
                guest.deprecated = false;
                guest.setRoles(Arrays.asList(Role.Query));

                userProfileRepository.saveAndFlush(guest);
            }
        });




        String pathToLoadFile = System.getProperty("ix.ginas.load.file");
        if (pathToLoadFile != null && substanceRepository.count()==0) {
            File f = new File(pathToLoadFile);
            if(f.exists()) {
                transactionTemplate.executeWithoutResult(status-> {
                    UserProfile up = userProfileRepository.findByUser_UsernameIgnoreCase("admin").standardize();

                    Authentication auth = new UsernamePasswordAuthenticationToken(up.user.username, null,
                            up.getRoles().stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r.name())).collect(Collectors.toList()));

                    SecurityContextHolder.getContext().setAuthentication(auth);
                });
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(pathToLoadFile))))) {
                    String line;
                    Pattern sep = Pattern.compile("\t");
                    ObjectMapper mapper = new ObjectMapper();
                    int i = 0;
                    while ((line = reader.readLine()) != null) {
                        String[] cols = sep.split(line);
//                System.out.println(cols[2]);
                        JsonNode json = mapper.readTree(cols[2]);
                        try {
                            transactionTemplate.executeWithoutResult(status-> {
                                try {
                                    substanceEntityService.createEntity(json, true).getCreatedEntity();
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
                }catch(Throwable t){
                    t.printStackTrace();
                }
            }else{
                System.err.println("could not find GSRS file: " + pathToLoadFile);
            }
        }

    }
}

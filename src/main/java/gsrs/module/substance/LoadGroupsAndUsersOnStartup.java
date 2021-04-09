package gsrs.module.substance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import gsrs.repository.GroupRepository;
import gsrs.repository.UserProfileRepository;
import gsrs.validator.GsrsValidatorFactory;
import ix.core.models.Group;
import ix.core.models.Principal;
import ix.core.models.Role;
import ix.core.models.UserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

@Profile("!test")
@Component
public class LoadGroupsAndUsersOnStartup implements ApplicationRunner {



//    @Autowired
//    GsrsValidatorFactory validationFactory;
    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private GroupRepository groupRepository;



    @Override
    public void run(ApplicationArguments args) throws Exception {
        if(groupRepository.count() >0){
            return;
        }
        groupRepository.save(new Group("protected"));

        System.out.println("RUNNING");

        UserProfile up = new UserProfile();
        up.user = new Principal("admin", "admin@example.com");
        up.setPassword("admin");
        up.active=true;
        up.deprecated=false;
        up.setRoles(Arrays.asList(Role.values()));

        userProfileRepository.saveAndFlush(up);

        UserProfile up2 = new UserProfile();
        up2.user = new Principal("user1", "user1@example.com");
        up2.setPassword("user1");
        up2.active=true;
        up2.deprecated=false;
        up2.setRoles(Arrays.asList(Role.Query));

        userProfileRepository.saveAndFlush(up2);

        UserProfile guest = new UserProfile();
        guest.user = new Principal("GUEST", "GUEST@example.com");
        guest.setPassword("GUEST");
        guest.active=false;
        guest.deprecated=false;
        guest.setRoles(Arrays.asList(Role.Query));

        userProfileRepository.saveAndFlush(guest);
    }
}

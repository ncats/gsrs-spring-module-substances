package example;

import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Only EnableAsync if we aren't in tests.  For the Substance-Example which can be run as an example
 * Substance module we want EnableAsync on, but for unit tests
 * we don't and we run most of the substance tests from this module.
 */
@EnableAsync
@Profile("!test")
public class AsyncForNotTestConfiguration {
}

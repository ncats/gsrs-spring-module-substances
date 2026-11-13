package ix.core;

import ix.core.util.FilteredPrintStream;
import ix.core.util.Filters;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class CoreStreamUtilitiesTest {

	@Test
	void filtersShouldMatchAndCombinePredictably() {
		StackTraceElement matching = new StackTraceElement(CoreStreamUtilitiesTest.class.getName(), "test", "CoreStreamUtilitiesTest.java", 1);
		StackTraceElement other = new StackTraceElement("example.Other", "test", "Other.java", 1);

		FilteredPrintStream.Filter classFilter = Filters.filterOutClasses(Pattern.compile(Pattern.quote(CoreStreamUtilitiesTest.class.getName())));
		assertFalse(classFilter.test(matching));
		assertTrue(classFilter.test(other));

		FilteredPrintStream.Filter anywhereFilter = Filters.filterOutAllClasses(Pattern.compile(Pattern.quote(CoreStreamUtilitiesTest.class.getName())));
		assertFalse(anywhereFilter.test(other));

		FilteredPrintStream.Filter allowMatchingOnly = stack -> stack.getClassName().startsWith(CoreStreamUtilitiesTest.class.getPackageName());
		FilteredPrintStream.Filter negated = allowMatchingOnly.not();
		assertFalse(negated.test(matching));
		assertTrue(negated.test(other));

		FilteredPrintStream.Filter combinedAnd = allowMatchingOnly.and(stack -> stack.getMethodName().equals("test"));
		assertTrue(combinedAnd.test(matching));
		assertFalse(combinedAnd.test(new StackTraceElement(CoreStreamUtilitiesTest.class.getName(), "other", "Test.java", 1)));

		FilteredPrintStream.Filter combinedOr = allowMatchingOnly.or(stack -> stack.getClassName().equals("example.Other"));
		assertTrue(combinedOr.test(other));
	}

	@Test
	void filterOutThreadShouldRejectCurrentThreadButAllowOtherThreads() throws Exception {
		FilteredPrintStream.Filter filter = Filters.filterOutThread(Thread.currentThread());

		assertFalse(filter.test(new StackTraceElement("irrelevant.Class", "method", "Class.java", 1)));

		AtomicBoolean otherThreadResult = new AtomicBoolean(false);
		Thread worker = new Thread(() -> otherThreadResult.set(filter.test(new StackTraceElement("irrelevant.Class", "method", "Class.java", 1))));
		worker.start();
		worker.join();

		assertTrue(otherThreadResult.get());
	}

	@Test
	void filteredPrintStreamShouldWriteNormallyAndRespectEnableDisable() throws Exception {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		FilteredPrintStream stream = new FilteredPrintStream(new PrintStream(buffer, true, StandardCharsets.UTF_8));

		stream.print("hello");
		stream.println(123);
		stream.format(" %s", "world");
		stream.append('!');
		stream.flush();

		String output = buffer.toString(StandardCharsets.UTF_8);
		assertTrue(output.contains("hello"));
		assertTrue(output.contains("123"));
		assertTrue(output.contains("world"));
		assertTrue(output.endsWith("!"));

		buffer.reset();
		stream.disableWriting(true);
		stream.println("suppressed");
		assertEquals("", buffer.toString(StandardCharsets.UTF_8));

		stream.enableWriting(true);
		stream.println("visible");
		assertTrue(buffer.toString(StandardCharsets.UTF_8).contains("visible"));
	}

	@Test
	void filteredPrintStreamShouldSwallowMatchingOutputAndRestoreAfterSessionClose() throws Exception {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		FilteredPrintStream stream = new FilteredPrintStream(new PrintStream(buffer, true, StandardCharsets.UTF_8));
		AtomicReference<Object> swallowed = new AtomicReference<>();

		try (FilteredPrintStream.FilterSession session = stream.newFilter(
				Filters.filterOutClasses(Pattern.compile(Pattern.quote(CoreStreamUtilitiesTest.class.getName()))))) {
			session.withOnSwallowed(swallowed::set);
			stream.println("hidden");
		}

		assertEquals("hidden", swallowed.get());
		assertEquals("", buffer.toString(StandardCharsets.UTF_8));

		stream.println("shown");
		assertTrue(buffer.toString(StandardCharsets.UTF_8).contains("shown"));
	}
}


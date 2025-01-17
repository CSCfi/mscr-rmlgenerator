package fi.csc.mscr.tranformation.rml;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RMLGeneratorTest {

	@Test
	void test() {
		RMLGenerator r = new RMLGenerator();
		assertEquals("test", r.testMe());
	}

}

package it.uniroma2.dicii.isw2.jcs.paramTests;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import org.junit.Before;

class TestCase {
	int a;
	
	@Before
	public void configure () {
		a = 2;
	}
	
	@Test
	void test() {
		assertEquals(a,2);
	}

}

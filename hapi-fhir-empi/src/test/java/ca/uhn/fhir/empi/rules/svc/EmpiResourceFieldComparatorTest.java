package ca.uhn.fhir.empi.rules.svc;

import ca.uhn.fhir.empi.BaseTest;
import ca.uhn.fhir.empi.rules.json.DistanceMetricEnum;
import ca.uhn.fhir.empi.rules.json.EmpiFieldMatchJson;
import ca.uhn.fhir.parser.DataFormatException;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Patient;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringStartsWith.startsWith;

public class EmpiResourceFieldComparatorTest extends BaseTest {
	protected EmpiResourceFieldComparator myComparator;
	private Patient myJohn;
	private Patient myJohny;

	@Before
	public void before() {
		super.before();

		myComparator = new EmpiResourceFieldComparator(ourFhirContext, myGivenNameMatchField);
		myJohn = buildJohn();
		myJohny = buildJohny();
	}

	@Test
	public void testBadType() {
		Encounter encounter = new Encounter();
		encounter.setId("Encounter/1");

		try {
			myComparator.match(encounter, myJohny);
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("Expecting resource type Patient got resource type Encounter", e.getMessage());
		}
		try {
			myComparator.match(myJohn, encounter);
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("Expecting resource type Patient got resource type Encounter", e.getMessage());
		}
	}

	@Test
	public void testBadPath() {
		try {
			EmpiFieldMatchJson matchField = new EmpiFieldMatchJson()
				.setName("patient-foo")
				.setResourceType("Patient")
				.setResourcePath("foo")
				.setMetric(DistanceMetricEnum.COSINE)
				.setMatchThreshold(NAME_THRESHOLD);
			EmpiResourceFieldComparator comparator = new EmpiResourceFieldComparator(ourFhirContext, matchField);
			comparator.match(myJohn, myJohny);
			fail();
		} catch (DataFormatException e) {
			assertThat(e.getMessage(), startsWith("Unknown child name 'foo' in element Patient"));
		}
	}

	@Test
	public void testMatch() {
		assertTrue(myComparator.match(myJohn, myJohny));
	}
}
package fi.csc.mscr.tranformation.rml;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.jupiter.api.Test;

class RMLGeneratorTest {

	Model loadPersonTestData() {
		Model model = ModelFactory.createDefaultModel();

		String[] inputFileNames = {"src/test/resources/data/person_json2shacl/crosswalk_content.ttl",
				"src/test/resources/data/person_json2shacl/crosswalk_metadata.ttl",
				"src/test/resources/data/person_json2shacl/source_metadata.ttl",
				"src/test/resources/data/person_json2shacl/source_content.ttl",
				"src/test/resources/data/person_json2shacl/target_metadata.ttl",
				"src/test/resources/data/person_json2shacl/target_content.ttl"
		};

		for (String inputFileName : inputFileNames) {
			model.add(RDFDataMgr.loadModel(inputFileName));
		}

		return model;
	}

	@Test
	void test() {
		RMLGenerator r = new RMLGenerator();
		assertEquals("test", r.testMe());
	}

	@Test
	void testAddLogicalSource() throws Exception {
		Model model = loadPersonTestData();
		RMLGenerator r = new RMLGenerator();
		Resource logicalSource = r.addLogicalSource("http://example.com/1", model);

		assertEquals("http://example.com/1", logicalSource.getURI());
		assertEquals("$", logicalSource.getProperty(model.createProperty("http://semweb.mmlab.be/ns/rml#iterator")).getObject().asLiteral().toString());
	}

}

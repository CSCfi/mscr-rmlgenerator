package fi.csc.mscr.tranformation.rml;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.jupiter.api.Test;

class RMLGeneratorTest {

	String [] personJsonTestData = {"src/test/resources/data/person_json2shacl/crosswalk_content.ttl",
			"src/test/resources/data/person_json2shacl/crosswalk_metadata.ttl",
			"src/test/resources/data/person_json2shacl/source_metadata.ttl",
			"src/test/resources/data/person_json2shacl/source_content.ttl",
			"src/test/resources/data/person_json2shacl/target_metadata.ttl",
			"src/test/resources/data/person_json2shacl/target_content.ttl"
	};

	Model loadPersonTestData(String[] inputFileNames) {
		Model model = ModelFactory.createDefaultModel();

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
	void testAddLogicalSourceForPersonJSON() throws Exception {
		Model model = loadPersonTestData(this.personJsonTestData);
		RMLGenerator r = new RMLGenerator();
		Resource logicalSource = r.addLogicalSource("http://example.com/1", model, "iterator:https://shacl-play.sparna.fr/shapes/Person");

		StmtIterator props = logicalSource.listProperties();
		while (props.hasNext()) {
			System.out.println(props.next());
		}

		assertEquals("http://example.com/1", logicalSource.getURI());
		assertEquals("$", logicalSource.getProperty(model.createProperty("http://semweb.mmlab.be/ns/rml#iterator")).getObject().asLiteral().toString());
	}

}

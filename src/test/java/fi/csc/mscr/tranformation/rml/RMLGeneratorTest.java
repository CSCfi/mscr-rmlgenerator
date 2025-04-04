package fi.csc.mscr.tranformation.rml;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;

class RMLGeneratorTest {

    String[] personJsonTestData = {"src/test/resources/data/person_json2shacl/crosswalk_content.ttl",
            "src/test/resources/data/person_json2shacl/crosswalk_metadata.ttl",
            "src/test/resources/data/person_json2shacl/source_metadata.ttl",
            "src/test/resources/data/person_json2shacl/source_content.ttl",
            "src/test/resources/data/person_json2shacl/target_metadata.ttl",
            "src/test/resources/data/person_json2shacl/target_content.ttl"
    };

    Model loadPersonTestData(List<String> inputFileNames) {
        Model model = ModelFactory.createDefaultModel();

        inputFileNames.forEach(inputFileName -> model.add(RDFDataMgr.loadModel(inputFileName)));

        return model;
    }

    @Test
    void test() {
        RMLGenerator r = new RMLGenerator();
        assertEquals("test", r.testMe());
    }

    @Test
    void testAddLogicalSourceForPersonJSON() throws Exception {
        Model model = loadPersonTestData(Arrays.asList(this.personJsonTestData));
        RMLGenerator r = new RMLGenerator();
        Resource logicalSource = r.addLogicalSource("http://example.com/1", model, "iterator:https://shacl-play.sparna.fr/shapes/Person", "mscr:crosswalk:653b47f8-0bad-4c0e-86e9-f4ff13b5d8e3");

        /*StmtIterator props = logicalSource.listProperties();
        props.forEach(
                System.out::println
        );*/

        assertEquals("http://example.com/1", logicalSource.getURI());
        assertEquals("http://semweb.mmlab.be/ns/rml#BaseSource", logicalSource.getProperty(model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")).getObject().toString());
		assertEquals("$", logicalSource.getProperty(model.createProperty("http://semweb.mmlab.be/ns/rml#iterator")).getObject().asLiteral().toString());
        assertEquals("http://semweb.mmlab.be/ns/ql#JSONPath", logicalSource.getProperty(model.createProperty("http://semweb.mmlab.be/ns/rml#referenceFormulation")).getObject().toString());
    }

    @Test
    void testAddSubjectMap() throws Exception {
        Model model = loadPersonTestData(Arrays.asList(this.personJsonTestData));
        RMLGenerator r = new RMLGenerator();

        Model subjMap = r.addSubjectMap(model, "subject:https://shacl-play.sparna.fr/shapes/Person");

        subjMap.write(System.out, "TTL");

    }

    @Test
    void testAddPredicateObjectMap() throws Exception {

        Model model = loadPersonTestData(Arrays.asList(this.personJsonTestData));
        RMLGenerator r = new RMLGenerator();

        Model predObjMaps = r.addPredicateObjectMap(model,"https://shacl-play.sparna.fr/shapes/Person");

        predObjMaps.write(System.out, "TTL");

    }

}

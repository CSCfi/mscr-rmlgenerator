package fi.csc.mscr.tranformation.rml;

import org.apache.jena.ext.xerces.util.URI;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDFS;

import org.topbraid.shacl.vocabulary.SH;

import java.io.InputStream;

//import fi.vm.yti.datamodel.api.v2.dto.MSCR;

public class RMLGenerator {

	String nsRML = "http://semweb.mmlab.be/ns/rml#";
	String nsQL = "http://semweb.mmlab.be/ns/ql#";
	String nsRR = "http://www.w3.org/ns/r2rml#";
	String nsFNO = "https://w3id.org/function/ontology#";
	String nsFNML = "http://semweb.mmlab.be/ns/fnml#";
	String nsGREL = "http://users.ugent.be/~bjdmeest/function/grel.ttl#";

	public String testMe() {

		Model model = ModelFactory.createDefaultModel();

		String[] inputFileNames = {"src/test/resources/data/person_json2shacl/crosswalk_content.ttl",
				"src/test/resources/data/person_json2shacl/crosswalk_metadata.ttl",
				"src/test/resources/data/person_json2shacl/source_metadata.ttl",
				"src/test/resources/data/person_json2shacl/source_content.ttl",
				"src/test/resources/data/person_json2shacl/target_metadata.ttl",
				"src/test/resources/data/person_json2shacl/target_content.ttl"
		};

		for (String inputFileName : inputFileNames) {

			//Model m = RDFDataMgr.loadModel("rmlgenerator/crosswalk1-input.ttl") ;
			model.add(RDFDataMgr.loadModel(inputFileName));

			// write out the Model

		}

		/*ResIterator subjects = model.listSubjectsWithProperty(RDF.type, model.getResource("http://uri.suomi.fi/datamodel/ns/mscr#Mapping"));

		System.out.println(model.size());

		while (subjects.hasNext()) {
			Resource res = subjects.next();
			System.out.println(res.getURI());
		}

		System.out.println("done");*/

		String q ="""
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>				
PREFIX : <http://uri.suomi.fi/datamodel/ns/mscr#>				
select ?iterator
where {
  ?mappingURI a <http://uri.suomi.fi/datamodel/ns/mscr#Mapping> .
  ?mappingURI :target/rdf:_1 ?target .
  ?target :uri ?<iterator:https://shacl-play.sparna.fr/shapes/Person> .
  
  ?mappingURI :source/rdf:_1 ?source .
  ?source :uri ?sourceShapeId .
  
  ?sourceShapeId <http://uri.suomi.fi/datamodel/ns/mscr#instancePath> ?iterator .
  
}						
				""";
		QueryExecution qe = QueryExecutionFactory.create(q, model);
		ResultSet results = qe.execSelect();


		while (results.hasNext()) {
			QuerySolution res = results.next();
			System.out.println(res.toString());
		}

		/*StmtIterator iter = model.listStatements(
				new SimpleSelector(null, RDF.type, (RDFNode) null) {
					public boolean selects(Statement s)
					{
						System.out.println(s.getObject());
						return s.getObject().toString() == "http://uri.suomi.fi/datamodel/ns/mscr#Mapping";}
				});

		System.out.println(iter.hasNext());*/

		//Resource test = model.getResource("https://example.com/1");
		//Literal prop = test.getProperty(ResourceFactory.createProperty("http://schema.org/name")).getLiteral();

		//System.out.println(prop);

		return "test";
	}

	public Resource addLogicalSourceModel(String logicalSourceURI, Model inputModel, Model m, String sourceSchemaURI,
										   String sourcePropertyURI) {

		/*
		You can now get the iterator source for a specific shape by querying along the lines:

		mapping -> target -> targetPropertyURI
		targetURI = <iterator:{uri of the shape}
		mapping -> source -> sourcePropertyURI
		sourcePropertyURI -> mscr:instancePath -> ?iteratorPath

		 */

		Resource logicalSource = m.createResource(logicalSourceURI);
		/*logicalSource.addProperty(RDF.type, m.createResource(nsRML + "BaseSource"));
		Resource referenceFormulation = m.createResource(nsQL + "JSONPath");
		logicalSource.addProperty(m.createProperty(nsRML + "referenceFormulation"), referenceFormulation);
		logicalSource.addProperty(m.createProperty(nsRML + "source"), m.createLiteral("data/person.json"));

		Resource sourceProperty = inputModel.getResource(sourcePropertyURI);
		Literal iteratorPath = sourceProperty.getProperty(MSCR.instancePath).getLiteral();
		logicalSource.addProperty(m.createProperty(nsRML + "iterator"), iteratorPath);*/

		return logicalSource;
	}
}

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
		return "test";
	}

	private String getSourceIteratorForTargetShape(Model model, String targetShapeUri) throws Exception {

		String q = String.format("""
				PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>				
				PREFIX : <http://uri.suomi.fi/datamodel/ns/mscr#>				
				select ?iterator
				where {
				  ?mappingURI a <http://uri.suomi.fi/datamodel/ns/mscr#Mapping> .
				  ?mappingURI :target/rdf:_1 ?target .
				  ?target :uri <%s> .
				
				  ?mappingURI :source/rdf:_1 ?source .
				  ?source :uri ?sourceShapeId .
				
				  ?sourceShapeId <http://uri.suomi.fi/datamodel/ns/mscr#instancePath> ?iterator .
				
				}						
				""", targetShapeUri);

		QueryExecution qe = QueryExecutionFactory.create(q, model);
		ResultSet results = qe.execSelect();


		if (results.hasNext()) {
			QuerySolution res = results.next();
			return res.getLiteral("iterator").toString();
		} else {
			throw new Exception("Source iterator could not be found for " + targetShapeUri);
		}

	}

	public Resource addLogicalSource(String logicalSourceURI, Model m) throws Exception {

		/*
		You can now get the iterator source for a specific shape by querying along the lines:

		mapping -> target -> targetPropertyURI
		targetURI = <iterator:{uri of the shape}
		mapping -> source -> sourcePropertyURI
		sourcePropertyURI -> mscr:instancePath -> ?iteratorPath

		 */

		String iterator = "";
		Resource logicalSource = m.createResource(logicalSourceURI);
		try {
			iterator = getSourceIteratorForTargetShape(m, "iterator:https://shacl-play.sparna.fr/shapes/Person");
		} catch(Exception e) {
			System.out.println(e.toString());
			throw e;
		}

		logicalSource.addProperty(m.createProperty(nsRML + "iterator"), iterator);

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

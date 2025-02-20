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
import java.util.Optional;

//import fi.vm.yti.datamodel.api.v2.dto.MSCR;

public class RMLGenerator {

	String nsRML = "http://semweb.mmlab.be/ns/rml#";
	String nsQL = "http://semweb.mmlab.be/ns/ql#";
	String nsRR = "http://www.w3.org/ns/r2rml#";
	String nsFNO = "https://w3id.org/function/ontology#";
	String nsFNML = "http://semweb.mmlab.be/ns/fnml#";
	String nsGREL = "http://users.ugent.be/~bjdmeest/function/grel.ttl#";

	enum ReferenceFormulation {
		JSONPath,
		XPath,
		CSV
	}

	public String testMe() {
		return "test";
	}

	private Optional<ReferenceFormulation> getSourceSchema(Model model, String crosswalkIri) {

		String q = String.format("""
				PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>				
				PREFIX : <http://uri.suomi.fi/datamodel/ns/mscr#>	
				
				select ?format where {
				    <%s> a :Crosswalk ;
				     :sourceSchema ?sourceIri .
				   
				    ?sourceIri :format ?format	   
				}		
				""", crosswalkIri);

		QueryExecution qe = QueryExecutionFactory.create(q, model);
		ResultSet results = qe.execSelect();

		if (results.hasNext()) {
			QuerySolution res = results.next();
			var ref = res.getLiteral("format");

			switch (ref.toString()) {
				case "JSONSCHEMA": return Optional.of(ReferenceFormulation.JSONPath);
				case "XSD": return Optional.of(ReferenceFormulation.XPath);
				case "CSV": return Optional.of(ReferenceFormulation.CSV);

				default: throw new Error("No matching reference formulation found for " + ref);
			}
		} else {
			return Optional.empty();
		}

	}

	private Optional<String> getSourceIteratorForTargetShape(Model model, String targetShapeUri) {

		String q = String.format("""
				PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>				
				PREFIX : <http://uri.suomi.fi/datamodel/ns/mscr#>				
				select ?iterator
				where {
				  ?mappingURI a :Mapping .
				  ?mappingURI :target/rdf:_1 ?target .
				  ?target :uri <%s> .
				
				  ?mappingURI :source/rdf:_1 ?source .
				  ?source :uri ?sourceShapeId .
				
				  ?sourceShapeId :instancePath ?iterator .
				}						
				""", targetShapeUri);

		QueryExecution qe = QueryExecutionFactory.create(q, model);
		ResultSet results = qe.execSelect();


		if (results.hasNext()) {
			QuerySolution res = results.next();
			return Optional.of(res.getLiteral("iterator").toString());
		} else {
			return Optional.empty();
		}

	}

	public Resource addLogicalSource(String logicalSourceURI, Model m, String targetShapeUri, String crosswalkIri) throws Exception {

		/*
		You can now get the iterator source for a specific shape by querying along the lines:

		mapping -> target -> targetPropertyURI
		targetURI = <iterator:{uri of the shape}
		mapping -> source -> sourcePropertyURI
		sourcePropertyURI -> mscr:instancePath -> ?iteratorPath

		 */

		String iterator = "";
		ReferenceFormulation refFormulation = null;
		Resource logicalSource = m.createResource(logicalSourceURI);

		try {
			var ref = this.getSourceSchema(m, crosswalkIri);
			refFormulation = ref.orElseThrow(Exception::new);
		} catch(Exception e) {
			System.out.println("Format could not be found");
			throw e;
		}
		
		logicalSource.addProperty(RDF.type, m.createResource(nsRML + "BaseSource"));
		Resource referenceFormulation = m.createResource(nsQL + refFormulation);
		logicalSource.addProperty(m.createProperty(nsRML + "referenceFormulation"), referenceFormulation);
		//logicalSource.addProperty(m.createProperty(nsRML + "source"), m.createLiteral("data/person.json"));

		try {
			iterator = this.getSourceIteratorForTargetShape(m, targetShapeUri).orElseThrow(Exception::new);
		} catch(Exception e) {
			System.out.println("Iterator could not be found");
			throw e;
		}

		logicalSource.addProperty(m.createProperty(nsRML + "iterator"), iterator);

		return logicalSource;
	}
}

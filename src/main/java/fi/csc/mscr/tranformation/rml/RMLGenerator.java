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
import java.util.UUID;

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

	private Optional<String> getSubjectMapTemplate(Model model, String mappingIri) {
		String q = String.format("""
				PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>				
				PREFIX : <http://uri.suomi.fi/datamodel/ns/mscr#>				
				
				select ?template
				where {
				    ?mappingURI a :Mapping .
				    ?mappingURI :target/rdf:_1 ?target .
				    ?target :uri <subject:https://shacl-play.sparna.fr/shapes/Person> .
				   
				    ?mappingURI :processing ?proc .
				    ?proc ?p ?o .
				    ?o rdf:_2 ?oo. # TODO: is this general?
				    ?oo :value ?template
				
				}
				
				""", mappingIri);

		QueryExecution qe = QueryExecutionFactory.create(q, model);
		ResultSet results = qe.execSelect();

		if (results.hasNext()) {
			QuerySolution res = results.next();
			return Optional.of(res.getLiteral("template").toString());
		} else {
			return Optional.empty();
		}

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

	private Model getPredicatesForShape(Resource triplesMap, Model model, String targetShapeUri) {

		String q = String.format("""
				PREFIX sh: <http://www.w3.org/ns/shacl#>
				PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>				
				PREFIX : <http://uri.suomi.fi/datamodel/ns/mscr#>				
				
				select ?sourceShapeUri ?schemaPath ?predicate
				where {
				    BIND(IRI("%s") as ?nodeShape)
				   
				    ?mappingURI a :Mapping .
				    ?mappingURI :target ?targetSeq .
				    ?targetSeq ?p ?target .
				    ?target :uri ?propShape .
				
				    ?nodeShape sh:property ?propShape  .
				    ?propShape sh:path ?predicate .
				
				    ?mappingURI :source ?seqBlankNode .
				    ?seqBlankNode ?pp ?sourceBlankNode .
				   
				    ?sourceBlankNode :uri ?sourceShapeUri .
				   
					?sourceShapeUri :schemaPath ?schemaPath
				}
				""", targetShapeUri);

		QueryExecution qe = QueryExecutionFactory.create(q, model);
		ResultSet results = qe.execSelect();

		Model outputModel = ModelFactory.createDefaultModel();

		while (results.hasNext()) {
			QuerySolution res = results.next();

			Resource predObjMap = outputModel.createResource();
			Resource objectMap = outputModel.createResource();

			Resource predicate = res.get("predicate").asResource();
			Literal reference = res.get("schemaPath").asLiteral();
			predObjMap.addProperty(outputModel.createProperty(nsRR + "predicate"), predicate);
			predObjMap.addProperty(outputModel.createProperty(nsRR + "objectMap"), objectMap);
			objectMap.addProperty(outputModel.createProperty(nsRML + "reference"), reference);

			outputModel.add(triplesMap, outputModel.createProperty(nsRR + "predicateObjectMap"), predObjMap);
		}

		return outputModel;

	}

	public Model addLogicalSource(Resource triplesMap, String logicalSourceURI, Model inputModel, String targetShapeUri, String crosswalkIri) throws Exception {

		Model outputModel = ModelFactory.createDefaultModel();

		/*
		You can now get the iterator source for a specific shape by querying along the lines:

		mapping -> target -> targetPropertyURI
		targetURI = <iterator:{uri of the shape}
		mapping -> source -> sourcePropertyURI
		sourcePropertyURI -> mscr:instancePath -> ?iteratorPath

		 */

		String iterator = "";
		ReferenceFormulation refFormulation = null;
		Resource logicalSource = outputModel.createResource(logicalSourceURI);

		try {
			var ref = this.getSourceSchema(inputModel, crosswalkIri);
			refFormulation = ref.orElseThrow(Exception::new);
		} catch(Exception e) {
			System.out.println("Format could not be found");
			throw e;
		}
		
		logicalSource.addProperty(RDF.type, outputModel.createResource(nsRML + "BaseSource"));
		Resource referenceFormulation = outputModel.createResource(nsQL + refFormulation);
		logicalSource.addProperty(outputModel.createProperty(nsRML + "referenceFormulation"), referenceFormulation);
		//logicalSource.addProperty(m.createProperty(nsRML + "source"), m.createLiteral("data/person.json"));

		try {
			iterator = this.getSourceIteratorForTargetShape(inputModel, targetShapeUri).orElseThrow(Exception::new);
		} catch(Exception e) {
			System.out.println("Iterator could not be found");
			throw e;
		}

		logicalSource.addProperty(outputModel.createProperty(nsRML + "iterator"), iterator);

		outputModel.add(triplesMap, outputModel.createProperty(nsRML + "logicalSource"), logicalSource);
		return outputModel;
	}

	public Model addSubjectMap(Resource triplesMap, Model inputModel, String mappingIri) throws Exception {

		Model outputModel = ModelFactory.createDefaultModel();


		Optional<String> template = getSubjectMapTemplate(inputModel, mappingIri);

		Resource subjMap = outputModel.createResource();
		subjMap.addProperty(outputModel.createProperty(nsRR + "template"), outputModel.createLiteral(template.orElseThrow(Exception::new)));
		subjMap.addProperty(outputModel.createProperty(nsRR + "class"), outputModel.createResource("http://schema.org/Person")); // TODO: get this from target

		triplesMap.addProperty(outputModel.createProperty(nsRR + "subjectMap"), subjMap);

		return outputModel;
	}

	public Model addPredicateObjectMap(Resource triplesMap, Model inputModel, String targetShapeUri) {

		return getPredicatesForShape(triplesMap, inputModel, targetShapeUri);

	}

	public Model generateMapping(String crosswalkUri, Model model) throws Exception {
		
		Model mapping = ModelFactory.createDefaultModel();
		// get tripleobjectmaps 
		String iteratorQuery = """
				PREFIX mscr: <http://uri.suomi.fi/datamodel/ns/mscr#> 
				PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>	
				select distinct ?target ?mapping
				where {
					?mapping a mscr:IteratorMapping .					
					?mapping mscr:target/rdf:_1/mscr:uri ?target .
					?mapping mscr:source/rdf:_1/mscr:uri ?source .
				}
				
				""";
		QueryExecution qe = QueryExecutionFactory.create(iteratorQuery, model);
		ResultSet it = qe.execSelect();
		while (it.hasNext()) {
			QuerySolution soln1 = it.next();

			String target = soln1.getResource("target").getURI();			
			String targetShapeURI = target.substring(9);
			String mappingURI = soln1.getResource("mapping").getURI();
			
			Resource triplesMap = mapping.createResource("http://rmlgenerator/" + UUID.randomUUID().toString());
			triplesMap.addProperty(RDF.type, mapping.createResource(nsRR + "TriplesMap"));
			
			String logicalSourceURI = triplesMap.getURI() + ":logicalSource";
			mapping.add(addLogicalSource(triplesMap, logicalSourceURI, model, target, crosswalkUri));			
			mapping.add(addPredicateObjectMap(triplesMap, model, targetShapeURI));
			mapping.add(addSubjectMap(triplesMap, model, mappingURI));
		
		}
		return mapping;
	}
}

package fi.vm.yti.datamodel.api.v2.transformation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;

import fi.vm.yti.datamodel.api.v2.dto.MSCR;
import fi.vm.yti.datamodel.api.v2.dto.MappingInfoDTO;
import fi.vm.yti.datamodel.api.v2.dto.NodeInfo;
import fi.vm.yti.datamodel.api.v2.mapper.MappingMapper;

public class RMLGenerator2 {

	String nsRML = "http://semweb.mmlab.be/ns/rml#";
	String nsQL = "http://semweb.mmlab.be/ns/ql#";
	String nsRR = "http://www.w3.org/ns/r2rml#";
	String nsFNO = "https://w3id.org/function/ontology#";
	String nsFNML = "http://semweb.mmlab.be/ns/fnml#";
	String nsGREL = "http://users.ugent.be/~bjdmeest/function/grel.ttl#";
	

	
	record IteratorData(String iteratorPropertyURI, String targetShapeURI, String sourcePropertyURI, String mappingURI) {}
	

	private String generateRandomURI(String suffix, String prefix) {
		return suffix + ":" + UUID.randomUUID().toString() + ":" + prefix;
	}
	public Model generateRMLFromMSCRGraph(Model inputModel, String crosswalkURI, String sourceSchemaURI) {
		Model m = ModelFactory.createDefaultModel();
		List<IteratorData> iterators = getIterators(inputModel);
		for (IteratorData iteratorData : iterators) {
		    String triplesMapURI = generateRandomURI("triplesMap", "");
    		String logicalSourceURI = generateRandomURI("logicalSource", "");
			String subjectMapURI = generateRandomURI("subjectMap", "");
			//String predicateObjecMapURI = generateRandomURI("", "");
			
			Resource triplesMap = m.createResource(triplesMapURI);
			triplesMap.addProperty(RDF.type, m.createResource(nsRR + "TriplesMap"));
			
			Resource logicalSource = addLogicalSourceModel(logicalSourceURI, inputModel, m, sourceSchemaURI, iteratorData.sourcePropertyURI);
			triplesMap.addProperty(m.createProperty(nsRML + "logicalSource"), logicalSource);
			
			
			Resource subjectMap = addSubjectMapModel(subjectMapURI, iteratorData.iteratorPropertyURI, iteratorData.targetShapeURI, inputModel, m);
			triplesMap.addProperty(m.createProperty(nsRR + "subjectMap"), subjectMap);
			
			addPredicateObjectMapModel(triplesMap, inputModel, m, iteratorData.targetShapeURI);
	
		}
		return m;
	}

	private void addPredicateObjectMapModel(Resource triplesMap, Model inputModel, Model outputModel, String targetShapeURI) {
		MappingMapper mapper = new MappingMapper();
		List<Resource> mappings = getPredicateObjectMappings(inputModel, targetShapeURI);
		for (Resource mappingResource : mappings) {
			MappingInfoDTO mappingInfo = mapper.mapToMappingDTO(mappingResource.getURI(), inputModel);
			if(mappingInfo.getProcessing() == null && 
					mappingInfo.getSource().stream().filter(NodeInfo::hasProcessing).collect(Collectors.toList()).isEmpty() &&
					mappingInfo.getTarget().stream().filter(NodeInfo::hasProcessing).collect(Collectors.toList()).isEmpty()) {
				for (NodeInfo targetNode : mappingInfo.getTarget()) {
					// there should be only one source property since there are no functions involved
					Resource pom = outputModel.createResource();
					Literal sourcePath = inputModel.getResource(mappingInfo.getSource().get(0).getId()).getProperty(MSCR.instancePath).getLiteral();
					Resource pomObjectMap = outputModel.createResource();
					pomObjectMap.addProperty(outputModel.createProperty(nsRML+"reference"), outputModel.createLiteral(sourcePath.getString()));
					pom.addProperty(outputModel.createProperty(nsRR+"objectMap"), pomObjectMap);
					
					Resource predicate = inputModel.getResource(targetNode.getId());
					pom.addProperty(outputModel.createProperty(nsRR+"predicate"), predicate);
					
					triplesMap.addProperty(outputModel.createProperty(nsRR + "predicateObjectMap"), pom);					
				}								
			}
			else {
				Resource sourceFunc = addSourceFuncModel(outputModel, mappingInfo.getSource());
				Resource mappingFunc = addMappingFuncModel(outputModel, mappingInfo, sourceFunc);
				
				for (int targetIndex = 0; targetIndex < mappingInfo.getTarget().size(); targetIndex++) {
					NodeInfo targetNode = mappingInfo.getTarget().get(targetIndex);
					Resource targetFunc = addTargetFuncModel(outputModel, targetNode, targetIndex, mappingFunc);
						
					Resource pom = outputModel.createResource();
					pom.addProperty(outputModel.createProperty(nsRR+"objectMap"), targetFunc);
					
					Resource predicate = inputModel.getResource(targetNode.getId());
					pom.addProperty(outputModel.createProperty(nsRR+"predicate"), predicate);
					
					triplesMap.addProperty(outputModel.createProperty(nsRR + "predicateObjectMap"), pom);					
					
				}
			}	

			addPredicateObjectMapProcessing(triplesMap, mappingInfo, inputModel, outputModel);
		}
	}
	private void addPredicateObjectMapProcessing(Resource triplesMap, MappingInfoDTO mappingInfo, Model inputModel, Model outputModel) {
		Resource mappingFuncValue = null;
		for (NodeInfo targetNode : mappingInfo.getTarget()) {
			
		}
		
	}
	private List<Resource> getPredicateObjectMappings(Model inputModel, String targetShapeURI) {
		String q ="""
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>				
PREFIX : <http://uri.suomi.fi/datamodel/ns/mscr#>	
prefix sh:    <http://www.w3.org/ns/shacl#> 			
select ?mappingURI
where {
   bind(URI("%s") as ?targetShape)
  ?mappingURI a :Mapping .
  ?mappingURI :target/(<>|!<>)*/:id ?targetPropertyURI .
  bind(URI(?targetPropertyURI) as ?targetProperty)
  ?targetShape sh:property ?targetProperty .
}						
				""".formatted(targetShapeURI);
		QueryExecution qe = QueryExecutionFactory.create(q, inputModel);
		ResultSet results = qe.execSelect();		
		List<Resource> list = new ArrayList<Resource>();
		while(results.hasNext()) {
			QuerySolution soln = results.next();
			Resource mapping = soln.get("mappingURI").asResource();
			list.add(mapping);
		}
		
		return list;
	}
	private Resource addSubjectMapModel(String subjectMapURI, String targetShapeURI, String targetClass, Model inputModel, Model outputModel) {
		String mappingURI = getSubjectSourceMapping(targetShapeURI, inputModel);
		if(mappingURI == null) {
			// no subject source mapping -> blank nodes 
			return null;
		}
		else {
			MappingMapper mapper = new MappingMapper();
			MappingInfoDTO mappingInfo = mapper.mapToMappingDTO(mappingURI, inputModel);
			// there should be only one
			NodeInfo sourcePropertyInfo = mappingInfo.getSource().get(0);
			Resource processingURI = addProcessingModel(outputModel, mappingInfo, 0);
			if(processingURI == null) {
				Resource subjectMap = outputModel.createResource(subjectMapURI);
				Literal sourcePath = inputModel.getResource(sourcePropertyInfo.getId()).getProperty(MSCR.instancePath).getLiteral();
				subjectMap.addProperty(outputModel.createProperty(nsRML+"reference"), sourcePath);
				subjectMap.addProperty(outputModel.createProperty(nsRR + "class"), outputModel.createResource(targetClass));
				subjectMap.addProperty(outputModel.createProperty(nsRR + "termType"), outputModel.createResource(nsRR + "IRI"));
				return subjectMap;
			}
			else {
				processingURI.addProperty(outputModel.createProperty(nsRR + "class"), outputModel.createResource(targetClass));

				processingURI.addProperty(outputModel.createProperty(nsRR + "termType"), outputModel.createResource(nsRR + "IRI"));
				return processingURI;
			}
			
		}
	}
	
	
	private Resource addProcessingModel(Model outputModel, MappingInfoDTO mappingInfo, int targetIndex) {
		if(mappingInfo.getProcessing() == null && 
				mappingInfo.getSource().stream().filter(NodeInfo::hasProcessing).collect(Collectors.toList()).isEmpty() &&
				mappingInfo.getTarget().stream().filter(NodeInfo::hasProcessing).collect(Collectors.toList()).isEmpty()) {
			return null;
		}
		Resource sourceFunc = addSourceFuncModel(outputModel, mappingInfo.getSource());
		Resource mappingFunc = addMappingFuncModel(outputModel, mappingInfo, sourceFunc);
		Resource targetFunc = addTargetFuncModel(outputModel, mappingInfo.getTarget().get(targetIndex), targetIndex, mappingFunc);
		return targetFunc;
	}

	private Resource addTargetFuncModel(Model outputModel, NodeInfo targetPropertyInfo, int targetIndex, Resource mappingFunc) {
		String targetFunctionURI = null;
		Map<String, Object> params = null;
		if(targetPropertyInfo.getProcessing() != null) {
			targetFunctionURI = targetPropertyInfo.getProcessing().getId();
			params = targetPropertyInfo.getProcessing().getParams();
			Resource outerFunctionResource = outputModel.createResource(generateRandomURI(targetPropertyInfo.getId(), ":outerfunc"));

			Resource function2Value = outputModel.createResource();
			outerFunctionResource.addProperty(outputModel.createProperty(nsFNML + "functionValue"), function2Value);
			
			Resource funcPom1_2 = outputModel.createResource();
			function2Value.addProperty(outputModel.createProperty(nsRR+"predicateObjectMap"), funcPom1_2);							
			funcPom1_2.addProperty(outputModel.createProperty(nsRR+"predicate"), outputModel.createResource(nsFNO+"executes"));
			Resource funcPom1ObjectMap2 = outputModel.createResource();
			funcPom1ObjectMap2.addProperty(outputModel.createProperty(nsRR+"constant"), outputModel.createResource(targetFunctionURI));
			funcPom1_2.addProperty(outputModel.createProperty(nsRR+"objectMap"), funcPom1ObjectMap2);
			
							
			Resource funcPom2_2 = outputModel.createResource();
			function2Value.addProperty(outputModel.createProperty(nsRR+"predicateObjectMap"), funcPom2_2);							
			funcPom2_2.addProperty(outputModel.createProperty(nsRR+"predicate"), outputModel.createResource(nsGREL+"anyObjectParam"));
			funcPom2_2.addProperty(outputModel.createProperty(nsRR+"objectMap"), mappingFunc);
			
			if(params != null) {
				for (String key : params.keySet()) {
					if(key.equals("input")) {
						continue;
					}
					Object value = params.get(key);
					Resource funcPom = outputModel.createResource();
					function2Value.addProperty(outputModel.createProperty(nsRR+"predicateObjectMap"), funcPom);							
					funcPom.addProperty(outputModel.createProperty(nsRR+"predicate"), outputModel.createResource(key));
					Resource funcPomObjectMap = outputModel.createResource();
					funcPomObjectMap.addProperty(outputModel.createProperty(nsRR+"constant"), outputModel.createLiteral(value.toString()));
					funcPom.addProperty(outputModel.createProperty(nsRR+"objectMap"), funcPomObjectMap);

				}
				
			}		
			return outerFunctionResource;
		}
		else {
			return mappingFunc;
		}

		
		
		
		

	}
	private Resource addMappingFuncModel(Model outputModel, MappingInfoDTO mappingInfo, Resource sourceFunctionResource) {
		String mappingFunctionURI = null;
		Map<String, Object> params = null;
		if(mappingInfo.getProcessing() != null) {
			mappingFunctionURI = mappingInfo.getProcessing().getId();
			params = mappingInfo.getProcessing().getParams();
			
			Resource mappingFunctionResource = outputModel.createResource(mappingInfo.getPID() +  ":func");
			//addOuterFunctionModel(outputModel, mappingFunctionResource, mappingFunctionURI, sourceFunctionResource, null);
			Resource functionValue = outputModel.createResource();
			mappingFunctionResource.addProperty(outputModel.createProperty(nsFNML + "functionValue"), functionValue);
			
			Resource funcPom1 = outputModel.createResource();
			functionValue.addProperty(outputModel.createProperty(nsRR+"predicateObjectMap"), funcPom1);							
			funcPom1.addProperty(outputModel.createProperty(nsRR+"predicate"), outputModel.createResource(nsFNO+"executes"));
			Resource funcPom1ObjectMap = outputModel.createResource();
			funcPom1ObjectMap.addProperty(outputModel.createProperty(nsRR+"constant"), outputModel.createResource(mappingFunctionURI));
			funcPom1.addProperty(outputModel.createProperty(nsRR+"objectMap"), funcPom1ObjectMap);

			Resource funcPom3 = outputModel.createResource();
			functionValue.addProperty(outputModel.createProperty(nsRR+"predicateObjectMap"), funcPom3);							
			funcPom3.addProperty(outputModel.createProperty(nsRR+"predicate"), outputModel.createResource(nsGREL+"p_param_a2"));
			funcPom3.addProperty(outputModel.createProperty(nsRR+"objectMap"), sourceFunctionResource);		

			if(params != null) {
				for (String key : params.keySet()) {
					if(key.equals("input")) {
						continue;
					}
					Object value = params.get(key);
					Resource funcPom = outputModel.createResource();
					functionValue.addProperty(outputModel.createProperty(nsRR+"predicateObjectMap"), funcPom);							
					funcPom.addProperty(outputModel.createProperty(nsRR+"predicate"), outputModel.createResource(key));
					Resource funcPomObjectMap = outputModel.createResource();
					funcPomObjectMap.addProperty(outputModel.createProperty(nsRR+"constant"), outputModel.createLiteral(value.toString()));
					funcPom.addProperty(outputModel.createProperty(nsRR+"objectMap"), funcPomObjectMap);

				}
				
			}
			return mappingFunctionResource;
		}	
		else {
			return sourceFunctionResource;
		}
		
		
		
	}
	private Resource addSourceFuncModel(Model outputModel, List<NodeInfo> source) {
		Resource latestFunction = null;
		for(int i = 0; i < source.size(); i++) {
			NodeInfo sourcePropertyInfo = source.get(i);
			String sourceFunctionURI = null;
			Map<String, Object> params = null;
			if(sourcePropertyInfo.getProcessing() != null) {
				sourceFunctionURI = sourcePropertyInfo.getProcessing().getId();
				params = sourcePropertyInfo.getProcessing().getParams();
			}
			 
			String outerFunctionURI = "http://users.ugent.be/~bjdmeest/function/grel.ttl#createArray";
			if(i > 0) {
				outerFunctionURI = "http://users.ugent.be/~bjdmeest/function/grel.ttl#addToArray";
			}
			Resource sourceFunctionResource = null;
			Resource outerFunctionResource = outputModel.createResource(sourcePropertyInfo.getId() + ":" + source.hashCode() + ":outerfunc");
			if(sourceFunctionURI != null) {
				sourceFunctionResource = outputModel.createResource(sourcePropertyInfo.getId() + ":" + source.hashCode() + ":func");				
				addFunctionModel(outputModel, sourceFunctionResource, sourceFunctionURI, sourcePropertyInfo.getInstancePath(), params);
				
			}
			else {
				sourceFunctionResource = outputModel.createResource();
				sourceFunctionResource.addProperty(outputModel.createProperty(nsRML+"reference"), outputModel.createLiteral(sourcePropertyInfo.getInstancePath()));
				
			}
			addOuterFunctionModel(outputModel, outerFunctionResource, outerFunctionURI, sourceFunctionResource, latestFunction);
			
			latestFunction = outerFunctionResource;
		}
		return latestFunction;
	}
	private void addOuterFunctionModel(Model m, Resource outerFunctionResource, String outerFunctionURI, Resource sourceFunctionResource, Resource prev) {
		Resource functionValue = m.createResource();
		outerFunctionResource.addProperty(m.createProperty(nsFNML + "functionValue"), functionValue);
		
		Resource funcPom1 = m.createResource();
		functionValue.addProperty(m.createProperty(nsRR+"predicateObjectMap"), funcPom1);							
		funcPom1.addProperty(m.createProperty(nsRR+"predicate"), m.createResource(nsFNO+"executes"));
		Resource funcPom1ObjectMap = m.createResource();
		funcPom1ObjectMap.addProperty(m.createProperty(nsRR+"constant"), m.createResource(outerFunctionURI));
		funcPom1.addProperty(m.createProperty(nsRR+"objectMap"), funcPom1ObjectMap);
		
		Resource funcPom2 = m.createResource();
		functionValue.addProperty(m.createProperty(nsRR+"predicateObjectMap"), funcPom2);							
		funcPom2.addProperty(m.createProperty(nsRR+"predicate"), m.createResource(nsGREL+"anyObjectParam"));
		funcPom2.addProperty(m.createProperty(nsRR+"objectMap"), sourceFunctionResource);
	
		if(prev != null) {
			Resource funcPom3 = m.createResource();
			functionValue.addProperty(m.createProperty(nsRR+"predicateObjectMap"), funcPom3);							
			funcPom3.addProperty(m.createProperty(nsRR+"predicate"), m.createResource(nsGREL+"p_param_a2"));
			funcPom3.addProperty(m.createProperty(nsRR+"objectMap"), prev);		
			
		}
		//sourceFunctionResource.addProperty(m.createProperty(nsRR + "class"), targetOntologyClass);
		//outerFunctionResource.addProperty(m.createProperty(nsRR + "termType"), m.createResource(nsRR + "IRI"));
		
	}

	private void addFunctionModel(Model m, Resource sourceFunctionResource, String sourceFunctionURI,
			String instancePath, Map<String, Object> params) {
	
		Resource functionValue = m.createResource();
		sourceFunctionResource.addProperty(m.createProperty(nsFNML + "functionValue"), functionValue);
		
		Resource funcPom1 = m.createResource();
		functionValue.addProperty(m.createProperty(nsRR+"predicateObjectMap"), funcPom1);							
		funcPom1.addProperty(m.createProperty(nsRR+"predicate"), m.createResource(nsFNO+"executes"));
		Resource funcPom1ObjectMap = m.createResource();
		funcPom1ObjectMap.addProperty(m.createProperty(nsRR+"constant"), m.createResource(sourceFunctionURI));
		funcPom1.addProperty(m.createProperty(nsRR+"objectMap"), funcPom1ObjectMap);
		
						
		Resource funcPom2 = m.createResource();
		functionValue.addProperty(m.createProperty(nsRR+"predicateObjectMap"), funcPom2);							
		funcPom2.addProperty(m.createProperty(nsRR+"predicate"), m.createResource(nsGREL+"anyObjectParam"));
		Resource funcPom2ObjectMap = m.createResource();
		funcPom2ObjectMap.addProperty(m.createProperty(nsRML+"reference"), m.createLiteral(instancePath));
		funcPom2.addProperty(m.createProperty(nsRR+"objectMap"), funcPom2ObjectMap);
		
		if(params != null) {
			for (String key : params.keySet()) {
				if(key.equals("input")) {
					continue;
				}
				Object value = params.get(key);
				Resource funcPom = m.createResource();
				functionValue.addProperty(m.createProperty(nsRR+"predicateObjectMap"), funcPom);							
				funcPom.addProperty(m.createProperty(nsRR+"predicate"), m.createResource(key));
				Resource funcPomObjectMap = m.createResource();
				funcPomObjectMap.addProperty(m.createProperty(nsRR+"constant"), m.createLiteral(value.toString()));
				funcPom.addProperty(m.createProperty(nsRR+"objectMap"), funcPomObjectMap);

			}
			
		}			

		//sourceFunctionResource.addProperty(m.createProperty(nsRR + "termType"), m.createResource(nsRR + "IRI"));		
		
	}
	private String getSubjectSourceMapping(String targetShapeURI, Model m) {
		String q ="""
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>				
PREFIX : <http://uri.suomi.fi/datamodel/ns/mscr#>				
select ?mappingURI
where {
  ?mappingURI a :Mapping .
  ?mappingURI :target/rdf:_1 ?target .
  ?target :id ?targetShapeURI .
  ?target :label ?targetLabel .
  FILTER(?targetLabel=\"subject source\")
}						
				""";
		QueryExecution qe = QueryExecutionFactory.create(q, m);
		ResultSet results = qe.execSelect();
		if(!results.hasNext()) {
			return null;
		}
		else {
			return results.next().get("mappingURI").asResource().getURI();
		}
	}
	private Resource addLogicalSourceModel(String logicalSourceURI, Model inputModel, Model m, String sourceSchemaURI,
			String sourcePropertyURI) {
		
		Resource logicalSource = m.createResource(logicalSourceURI);
		logicalSource.addProperty(RDF.type, m.createResource(nsRML + "BaseSource"));
		Resource referenceFormulation = m.createResource(nsQL + "JSONPath");		
		logicalSource.addProperty(m.createProperty(nsRML + "referenceFormulation"), referenceFormulation);
		logicalSource.addProperty(m.createProperty(nsRML + "source"), m.createLiteral("data/person.json"));
		
		Resource sourceProperty = inputModel.getResource(sourcePropertyURI);
		Literal iteratorPath = sourceProperty.getProperty(MSCR.instancePath).getLiteral();
		logicalSource.addProperty(m.createProperty(nsRML + "iterator"), iteratorPath);
		
		return logicalSource;
	}
	private List<IteratorData> getIterators(Model inputModel) {
		String q ="""
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>				
PREFIX : <http://uri.suomi.fi/datamodel/ns/mscr#>				
select ?targetShapeURI ?sourcePropertyURI ?mappingURI
where {
  ?mappingURI a :Mapping .
  ?mappingURI :target/rdf:_1 ?target .
  ?mappingURI :source/rdf:_1/:id ?sourcePropertyURI .
  ?target :id ?targetShapeURI .
  ?target :label ?targetLabel .
  FILTER(?targetLabel=\"iterator source\")
}						
				""";
		QueryExecution qe = QueryExecutionFactory.create(q, inputModel);
		ResultSet results = qe.execSelect();
		
		List<IteratorData> list = new ArrayList<IteratorData>();
		while(results.hasNext()) {
			QuerySolution soln = results.next();
			String targetShapeURI = soln.get("targetShapeURI").asLiteral().getString();
			String targetClass = targetShapeURI.substring(9); // TODO: remove this 
			list.add(new IteratorData(
					targetShapeURI,
					targetClass,
					soln.get("sourcePropertyURI").asLiteral().getString(),
					soln.get("mappingURI").asResource().getURI()
			));
		}
		
		return list;
	}
}

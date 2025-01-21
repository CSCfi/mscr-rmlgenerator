# mscr-rmlgenerator

## Preconditions 
 1. Iterator mappings must be one-to-one mappings i.e. only only one property is mapped for source and target. (Is this true?)


## Pseudocode

def getReferenceFormulationResource(sourceSchemaURI, inputGraph)
  sourceSchemaMetadataResource = inputGraph.getResource(sourceSchemaURI)
  format = sourceSchemeMetadataResource.getPropertyValue(:format)
  switch(format)
    CSV:
      return new Resource(ql:CSV)
    XML:
      return new Resource(ql:XPath)
    JSON
      return new Resource(ql:JSONPath)

def getLogicalSourceData(g)
  return queryResults(g, 
    select ?targetProperty ?sourceProperty 
    where 
      ?mapping a :Mapping .
      ?mapping :target/rdf:_1 ?target # see precondition 1
      ?mapping :source/rdf:_1 ?source 
      ?target :id ?targetShape .
      ?source :id ?sourceProperty .
      
      ?target :mscrType ?targetType .
      filter(?targetType == "iterator")    
  )

def getLogicalSource(subjectURI, inputGraph, sourceSchemaURI) 
  logicalSource = new Graph()
  referenceFormulation = getReferenceFormulationResource(sourceSchemaURI, inputGraph)
  # create triples for RML LogicalSource
  # What to add for rml:source? 
  return logicalSource
  
def getSubjectMap(subjectURI):

def getPredicateObjectMap(subjectURI):

def generateRMLFromMSCRGraph(inputGraph)
  sourceSchemaURI = getSourceSchemaURI(inputGraph)
  outputGraph = new Graph()
  foreach(logicalSourceData in getLogicalSourceData(inputGraph))    
    triplesMapURI = new Resource(getTriplesMapURI()) # blank node
    logicalSourceURI = new Resource(getLogicalSourceURI())
    subjectMapURI = new Resource(getSubjectMapURI())
    predicateObjecMapURI = new Resource(getPredicateObjectMapURI())
    
    targetShape = logicalSourceData.targetShape
    sourceProperty = logicalSourceData.sourceProperty
    
    outputGraph.add(getLogicalSource(logicalSourceResource, inputGraph, sourceSchemaURI))
    outputGraph.add(rml:logicalSource, new Resource(logicalSourceURI))

    outputGraph.add(getSubjectMap(subjectMapURI))
    outputGraph.add(rr:subjectMap, new Resource(subjectMapURI))

    outputGraph.add(getPredicateObjectMap(predicateObjectMapURI))
    outputGraph.add(rr:predicateObjectMap, new Resource(predicateObjectMapURI))
 
  return outputGraph
    

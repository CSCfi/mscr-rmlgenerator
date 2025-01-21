# mscr-rmlgenerator

## Preconditions 
 1. Iterator mappings must be one-to-one mappings i.e. only only one property is mapped for source and target. (Is this true?)
 2. Subject source mapping must have only one target property (i.e. the generated subject source property)
    

## Pseudocode

```
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

# Returns a list of source propertyURIs that are used to reference of generate subject URIs for the given target shape
def getSubjectSourceProperties(inputGraph, targetShapeURI)
  return queryResults(inputGraph,
    select ?sourcePropertyURI
    where
     ?mapping a :Mapping
     ?mapping :target/rdf:_1 ?target
     ?mapping :source/*/:id ?sourcePropertyURI # can have multiple sources
     ?target :id targetShapeURI
     ?target :mscrType ?targetType .
     filter(?targetType == "subject")
  )

# Returns a list of values that represent all the iterator mappings in the crosswalk
def getIteratorData(g)
  return queryResults(g, 
    select ?targetShapeURI ?sourcePropertyURI
    where 
      ?mapping a :Mapping .
      ?mapping :target/rdf:_1 ?target # see precondition 1
      ?mapping :source/rdf:_1/:id ?sourcePropertyURI
      ?target :id ?targetShapeURI .
      ?target :mscrType ?targetType .
      filter(?targetType == "iterator")    
  )

def getLogicalSource(subjectURI, inputGraph, sourceSchemaURI, sourcePropertyURI) 
  logicalSource = new Graph()
  referenceFormulation = getReferenceFormulationResource(sourceSchemaURI, inputGraph)
  # create triples for RML LogicalSource
  iteratorPath = inputGraph.getPropertyValue(sourcePropertyURI, :instancePath)

  logicalSource.add(rml:referenceFormulation, referenceFormulation)
  logicalSource.add(rml:iterator, iteratorPath)
   # What to add for rml:source?
  logicalSource.add(rml:source, ?????)
  return logicalSource
  
def getSubjectMap(subjectURI, sourcePropertyURIs, inputGraph):
  subjectMap = new Graph()

def getPredicateObjectMap(subjectURI):

def generateRMLFromMSCRGraph(inputGraph)
  sourceSchemaURI = getSourceSchemaURI(inputGraph)
  outputGraph = new Graph()
  foreach(iteratorData in getIteratorData(inputGraph))    
    triplesMapURI = new Resource(getTriplesMapURI()) # blank node
    logicalSourceURI = new Resource(getLogicalSourceURI())
    subjectMapURI = new Resource(getSubjectMapURI())
    predicateObjecMapURI = new Resource(getPredicateObjectMapURI())
    
    targetShapeURI = iteratorData.targetShapeURI
    sourcePropertyURI = iteratorData.sourcePropertyURI
    
    outputGraph.add(getLogicalSource(logicalSourceResource, inputGraph, sourceSchemaURI, sourcePropertyURI))
    outputGraph.add(rml:logicalSource, new Resource(logicalSourceURI))

    outputGraph.add(
      getSubjectMap(
        subjectMapURI,
        getSubjectSourceProperties(inputGraph, targetShapeURI)
        inputGraph
      )
    )
    outputGraph.add(rr:subjectMap, new Resource(subjectMapURI))

    outputGraph.add(getPredicateObjectMap(predicateObjectMapURI))
    outputGraph.add(rr:predicateObjectMap, new Resource(predicateObjectMapURI))
 
  return outputGraph
``` 

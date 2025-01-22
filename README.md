# mscr-rmlgenerator

## Preconditions 
 1. Iterator mappings must be one-to-one mappings i.e. only only one property is mapped for source and target AND there can be only one iterator mapping per NodeShape. The reason fro this is that there is currently no way of grouping other mappings under specific iterator mapping. TODO: Add examples (screenshots from MSCR).
 2. Subject source mapping must have only one target property (i.e. the generated subject source property)
 3. There can only be one pair of iterator/subject source mappings per SHACL shape. How significant restriction is this?
 
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

# Returns an ordered list of source propertyURIs that are used to reference of generate subject URIs for the given target shape
def getSubjectSourceMapping(inputGraph, targetShapeURI)
  return queryResults(inputGraph,
    select ?mappingURI
    where
     ?mappingURI a :Mapping
     ?mappingURI :target/rdf:_1 ?target
     ?target :id targetShapeURI
     ?target :mscrType ?targetType .
     filter(?targetType == "subject")
    )

# Returns a list of values that represent all the iterator mappings in the crosswalk
def getIteratorData(g)
  return queryResults(g, 
    select ?targetShapeURI ?sourcePropertyURI ?mappingURI
    where 
      ?mappingURI a :Mapping .
      ?mappingURI :target/rdf:_1 ?target # see precondition 1
      ?mappingURI :source/rdf:_1/:id ?sourcePropertyURI
      ?target :id ?targetShapeURI .
      ?target :mscrType ?targetType .
      filter(?targetType == "iterator")    
  )

def getLogicalSourceModel(subjectURI, inputGraph, sourceSchemaURI, sourcePropertyURI) 
  logicalSource = new Graph()
  referenceFormulation = getReferenceFormulationResource(sourceSchemaURI, inputGraph)
  # create triples for RML LogicalSource
  iteratorPath = inputGraph.getPropertyValue(sourcePropertyURI, :instancePath)

  logicalSource.add(rml:referenceFormulation, referenceFormulation)
  logicalSource.add(rml:iterator, iteratorPath)
   # What to add for rml:source?
  logicalSource.add(rml:source, ?????)
  return logicalSource

def createFunctionCallModel()

def getProcessingModel(uri, inputGraph, mappingURI)
  mappingInfo = getMappingInfo(mappingURI)
  m = new Graph()

  function = m.createResource()
  functionValue = m.createResource()
  m.addTriple(function, fnml:functionValue, functionValue)
  


def getSourcePropertyURIs(mappingURI, inputGraph)
  return queryResults(inputGraph,
    select ?sourcePropertyID
    where
     mappingURI :source/* ?sourceProperty
     ?sourceProperty :id ?sourcePropertyID
     ?sourceProperty :index ?index
    order by ASC(?index)
   )

def getSubjectMapModel(uri, targetShapeURI, inputGraph):
  mappingURI = getSubjectSourceMapping(targetShapeURI, inputGraph)
  subjectMap = new Graph()

  sourcePropertyURIs = getSourcePropertyURIs(mappingURI, inputGraph)
  if(sourcePropertyURIs.isEmptyUI)
    # case: blank node
  else
    subjectMap.addTriple(uri, rdf.type, rr:SubjectMap)
    processingURI = getProcessingURI(...)
  
    processingModel = getProsessingModel(processingURI, inputGraph, mappingURI)
    if(processingModel != null)
      subjectMap.addModel(processingModel)
      subjectMap.addTriple(uri, fnml:functionValue, processingURI)
    else
      if(sourcePropertyURIs.size() > 1)
        throw new Exception("Subject source mapping without processing functions must have only one source")
      sourcePath = inputGraph.getString(sourceProeprtyURIs.get(0) :instancePath)
      subjectMap.addTriple(uri, rml:reference, sourcePath)
    # add also rr:class ?
    subjectMap.addTriple(uri, rr:tempType, rr:IRI)
  return subjectMap
 
def getPredicateObjectMapModel(subjectURI):

def generateRMLFromMSCRGraph(inputGraph)
  validate(inputGraph) # check if the preconditions hold
  sourceSchemaURI = getSourceSchemaURI(inputGraph)
  outputGraph = new Graph()
  foreach(iteratorData in getIteratorData(inputGraph))
    
    triplesMapURI = getTriplesMapURI()
    logicalSourceURI = getLogicalSourceURI()
    subjectMapURI = getSubjectMapURI()
    predicateObjecMapURI = getPredicateObjectMapURI()

    mappingURI = iteratorData.mappingURI
    targetShapeURI = iteratorData.targetShapeURI
    sourcePropertyURI = iteratorData.sourcePropertyURI
    
    outputGraph.addModel(getLogicalSourceModel(logicalSourceURI, inputGraph, sourceSchemaURI, sourcePropertyURI))
    outputGraph.addTriple(triplesMapURI, rml:logicalSource, logicalSourceURI)

    outputGraph.addModel(
      getSubjectMapModel(
        subjectMapURI
        inputGraph
      )
    )
    outputGraph.addTriple(triplesMapURI, rr:subjectMap, subjectMapURI)

    outputGraph.addModel(getPredicateObjectMapModel(predicateObjectMapURI))
    outputGraph.addTriple(triplesMapURI, rr:predicateObjectMap, predicateObjectMapURI)
 
  return outputGraph
``` 

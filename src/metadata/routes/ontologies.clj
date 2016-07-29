(ns metadata.routes.ontologies
  (:use [common-swagger-api.schema]
        [common-swagger-api.schema.ontologies]
        [metadata.routes.schemas.common]
        [metadata.routes.schemas.ontologies]
        [ring.util.http-response :only [ok]])
  (:require [metadata.services.ontology :as service]))

(defroutes* ontologies
  (context* "/ontologies" []
    :tags ["ontologies"]

    (GET* "/" []
          :query [{:keys [user]} StandardUserQueryParams]
          :return OntologyDetailsList
          :summary "List Ontology Details"
          :description "Lists Ontology details saved in the database."
          (ok (service/get-ontology-details-listing)))

    (GET* "/:ontology-version" []
          :path-params [ontology-version :- OntologyVersionParam]
          :query [{:keys [user]} StandardUserQueryParams]
          :return OntologyHierarchyList
          :summary "Get Ontology Hierarchies"
          :description "List Ontology Hierarchies saved for the given `ontology-version`."
          (ok (service/list-hierarchies ontology-version)))

    (POST* "/:ontology-version/filter" []
           :path-params [ontology-version :- OntologyVersionParam]
           :query [{:keys [user]} StandardUserQueryParams]
           :body [{:keys [type id attrs]} TargetHierarchyFilterRequest]
           :return OntologyHierarchyList
           :summary "Filter Target's Ontology Hierarchies"
           :description
           "Filters Ontology Hierarchies saved for the given `ontology-version`,
            returning only the hierarchy's leaf-classes that are associated with the given target."
           (ok (service/filter-target-hierarchies ontology-version attrs type id)))

    (POST* "/:ontology-version/:root-iri/filter" []
           :path-params [ontology-version :- OntologyVersionParam
                         root-iri :- OntologyClassIRIParam]
           :query [{:keys [attr user]} OntologyHierarchyFilterParams]
           :body [{:keys [target-types target-ids]} TargetFilterRequest]
           :return OntologyHierarchy
           :summary "Filter an Ontology Hierarchy"
           :description
           "Filters an Ontology Hierarchy, rooted at the given `root-iri`, returning only the
            hierarchy's leaf-classes that are associated with the given targets."
           (ok (service/filter-hierarchy ontology-version root-iri attr target-types target-ids)))

    (POST* "/:ontology-version/:root-iri/filter-unclassified" []
           :path-params [ontology-version :- OntologyVersionParam
                         root-iri :- OntologyClassIRIParam]
           :query [{:keys [attr user]} OntologyHierarchyFilterParams]
           :body [{:keys [target-types target-ids]} TargetFilterRequest]
           :return TargetIDList
           :summary "Filter Unclassified Targets"
           :description
           "Filters the given target IDs by returning a list of any that are not associated with any
            Ontology classes of the hierarchy rooted at the given `root-iri`."
           (ok (service/filter-unclassified-targets ontology-version root-iri attr target-types target-ids)))))

(defroutes* admin-ontologies
  (context* "/admin/ontologies" []
    :tags ["admin-ontologies"]

    (POST* "/" []
           :query [{:keys [user]} StandardUserQueryParams]
           :multipart-params [ontology-xml :- String]
           :middlewares [service/wrap-multipart-xml-parser]
           :return OntologyDetails
           :summary "Save an Ontology"
           :description "Saves an Ontology XML document in the database."
           (ok (service/save-ontology-xml user ontology-xml)))

    (DELETE* "/:ontology-version" []
             :path-params [ontology-version :- OntologyVersionParam]
             :query [{:keys [user]} StandardUserQueryParams]
             :summary "Delete an Ontology"
             :description "Marks an Ontology as deleted in the database."
             (ok (service/delete-ontology user ontology-version)))

    (DELETE* "/:ontology-version/:root-iri" []
             :path-params [ontology-version :- OntologyVersionParam
                           root-iri :- OntologyClassIRIParam]
             :query [{:keys [user]} StandardUserQueryParams]
             :return OntologyHierarchyList
             :summary "Delete an Ontology Hierarchy"
             :description
             "Removes the Ontology Class for the given `root-iri` and `ontology-version`, and all
              sub-classes saved under its hierarchy. The `root-iri` may be a root saved by the `PUT`
              endpoint, or any class IRI anywhere in its hierarchy.
              Deleted classes and sub-hierarchies can easily be re-added by re-saving the
              hierarchy's root with the `PUT` endpoint again."
             (ok (service/delete-hierarchy user ontology-version root-iri)))

    (PUT* "/:ontology-version/:root-iri" []
          :path-params [ontology-version :- OntologyVersionParam
                        root-iri :- OntologyClassIRIParam]
          :query [{:keys [user]} StandardUserQueryParams]
          :return OntologyHierarchy
          :summary "Save an Ontology Hierarchy"
          :description
          "Save an Ontology Hierarchy, parsed from the Ontology XML stored with the given
           `ontology-version`, rooted at the given `root-iri`.
           Only classes and hierarchies that are not already saved under the given `ontology-version` are
           added. Any `root-iri` saved with this endpoint (that does not already exist) will be listed as
           a distinct hierarchy root for the given `ontology-version`."
          (ok (service/save-hierarchy user ontology-version root-iri)))))

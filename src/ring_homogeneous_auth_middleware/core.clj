(ns ring-homogeneous-auth-middleware.core
  "This ns provide a middleware that could be used to merge potentially multiple
  auth middleware effects.

  Given a list of functions taking a ring request and returning a (s/maybe IdentityInfo)
  The `wrap-auths-fn` returns a middleware the add the first non nil response
  from those functions in the :identity-info key of the ring-request.

  Some helpers are also provided for compojure-api usage.
  "
  (:require [ring-homogeneous-auth-middleware.schemas :refer [IdentityInfo]]
            [clojure.set :as set]
            [compojure.api.meta :as meta]
            [schema.core :as s]))

(s/defn wrap-auths-fn
  "You should provide a list of [[AuthExtractor]]s your ring request should have
  a :auth key in them."
  [auth-extractors]
  (fn [handler]
    (fn [request]
      (let [identity-info
            (->> auth-extractors
                 (map #(% request))
                 (remove nil?)
                 first)
            new-request (if identity-info
                          (assoc request :identity-info identity-info)
                          request)]
        (handler new-request)))))

;; COMPOJURE-API Restructuring

;; Add the :identity-info in the route description
(defmethod meta/restructure-param
  :identity-info [_ id-infos acc]
  (let [schema  (meta/fnk-schema id-infos)
        new-letks [id-infos (meta/src-coerce! schema :identity-info :string)]]
    (update-in acc [:letks] into new-letks)))

;; Add the :scopes-filter
;; to compojure api params
;; it should contains a set of hash-maps
;; example:
;;
;; ~~~
;; (POST "/foo" [] :scopes-filter #{:admin})
;; ~~~
;;
;; Will be accepted only for requests having a scope in the authorized set.

(defn check-scopes-filter!
  [authorized-scopes request-scopes]
  (when-not (set? authorized-scopes)
    (throw (ex-info ":scopes-filter argument in compojure-api must be a set!" {})))
  (when-not (and (set? request-scopes)
                 (set/intersection authorized-scopes request-scopes))
    (ring.util.http-response/unauthorized!
     {:msg "You don't have the required credentials to access this route"})))

(defmethod compojure.api.meta/restructure-param
  :scopes-filter [_ authorized acc]
  (update-in
   acc
   [:lets]
   into
   ['_ `(check-scopes-filter!
         ~authorized
         (:identity-info ~'+compojure-api-request+))]))

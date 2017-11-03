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

(s/defn get-identity-info :- (s/maybe IdentityInfo)
  "Given a ring request and and a couple auth-key auth-info->identity-info.
  We return the identity-info if possible"
  [request
   [auth-key auth-infos->identity-info]]
  (when-let [auth-infos (get request auth-key)]
    (auth-infos->identity-info auth-infos)))

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

;; Add the :roles-filter
;; to compojure api params
;; it should contains a set of hash-maps
;; example:
;;
;; ~~~
;; (POST "/foo" [] :roles-filter #{:admin})
;; ~~~
;;
;; Will be accepted only for requests having a role in the authorized set.

(defn check-roles-filter!
  [authorized-roles request-roles]
  (when-not (set? authorized-roles)
    (throw (ex-info ":roles-filter argument in compojure-api must be a set!" {})))
  (when-not (and (set? request-roles)
                 (set/intersection authorized-roles request-roles))
    (ring.util.http-response/unauthorized!
     {:msg "You don't have the required credentials to access this route"})))

(defmethod compojure.api.meta/restructure-param
  :roles-filter [_ authorized acc]
  (update-in
   acc
   [:lets]
   into
   ['_ `(check-roles-filter!
         ~authorized
         (:identity-info ~'+compojure-api-request+))]))

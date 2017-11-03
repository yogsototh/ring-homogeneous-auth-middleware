(ns ring-homogeneous-auth-middleware.core-test
  (:require [ring-homogeneous-auth-middleware.core :as sut]
            [ring-homogeneous-auth-middleware.schemas :refer [IdentityInfo]]
            [clojure.test :as t :refer [is use-fixtures]]
            [schema.test :refer [deftest]]
            [schema.core :as s]))

(use-fixtures :once schema.test/validate-schemas)

;; Extractor code example for some JWT

(s/defn extract-identity-info :- IdentityInfo
  [jwt-info]
  {:user {:id (:sub jwt-info)
          :name (:sub jwt-info)}
   :groups #{{:id (:org_guid jwt-info)
              :name (:org_name jwt-info)}}
   :roles (if (= "true"
                 ;; this test handle the case when :admin is a string
                 ;; and when its a boolean
                 (str (:admin jwt-info)))
            #{:admin :user}
            #{:user})
   :auth-type :jwt
   :jwt jwt-info})

(s/defn jwt-extractor :- (s/maybe IdentityInfo)
  [req]
  (some-> req
          :jwt
          extract-identity-info))

;; Extractor code example for API Key

(s/defn api-key-extractor :- (s/maybe IdentityInfo)
  [req]
  (some-> req
          :api-key-infos
          (assoc :auth-type :api-key)))

;; Tests

(deftest wrap-auths-test
  (let [base-request {:server-port 8080
                      :server-name "localhost"
                      :remote-addr "127.0.0.1"
                      :uri "/"
                      :scheme :http
                      :request-method :get
                      :protocol "HTTP/1.1"
                      :headers {}}

        jwt {:admin true
             :sub "testuser@cisco.com"
             :org_name "IROH Testing"
             :org_guid "00000000-0000-0000-00000000000000000"
             :nbf 1487167750
             :jti "aaaaaaaa-aaaa-aaaa-aaaaaaaaaaaaaaaaa"
             :iat 1487168050
             :exp 1487772850}

        id-info {:user {:id "testuser@cisco.com"
                        :name "testuser@cisco.com"}
                 :groups #{{:id "00000000-0000-0000-00000000000000000"
                            :name "IROH Testing"}}
                 :roles #{:admin :user}
                 :auth-type :jwt
                 :jwt jwt}

        request-jwt (assoc base-request :jwt jwt)
        request-api-key (assoc base-request :api-key-infos id-info)
        app ((sut/wrap-auths-fn [jwt-extractor api-key-extractor]) identity)]
    (is (nil? (:identity-info (app base-request)))
        "without any :jwt nor :api-key there shouldnt be any identity-info")
    (is (= (:identity-info (app request-jwt))
           id-info)
        "Should provide identity-info from a jwt field")
    (is (= (:identity-info (app request-api-key))
           (assoc id-info :auth-type :api-key))
        "Should provide identity-info from a api-key field")))

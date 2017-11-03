# ring-homogeneous-auth-middleware

A Clojure library designed to homogenise many different auth middleware.

## Usage

Generally each auth middleware add the auth informations to the ring-request
hash-map.

So for example a ring-jwt-auth middleware will add a `:jwt` field
containing some informations about the identity and auth details.

Another middleware could also be used, for example one might want
to accept JWT and long term API keys. The other middleware could then
add a `:api-key-infos` field to the hash-map whose value could be
some other kind of information.

This middleware is a simple way to merge all those different informations
in a centralized and normalized way.
The middelware takes multiple _extractors_ as parameters.
An extractor is a function that given a ring-request extract an `IdentityInfo` or nil.

An `IdentityInfo` is defined as:

```clojure
(s/defschema User
  "An User should be understood as a unique entity able to be identified.
  An user must have an unique id and also a name.

  An user could also contain many meta fields that could be provided as meta
  data by some authentication layer. Typically, an email, a phone number, etc...
  "
  (st/merge
   {:id s/Str
    :name s/Str}
   ;; could contain other meta datas (email, address, phone number, etc...)
   {s/Keyword s/Any}))

(s/defschema Group
  "A Group can be understood as a Community of People, an Organization, a
  Business, etc...

  Mainly this should provide a way to filter document for an organization.

  A group must have an unique identifier and a name.

  A group could also have some meta informations. For example, a physical
  address, an Identity Provider URL, etc.."
  (st/merge
   {:id s/Str
    :name s/Str}
   ;; could contain other meta datas (Identity Provider URL, etc...)
   {s/Keyword s/Any}))

(def Role
  "What are the roles of the user.

  Mainly this should provide a way to filter route access.

  Typical values are: :admin :user :read-only etc... "
  s/Keyword)

(s/defschema IdentityInfo
  "An IdentityInfo provide the information to identify and determine the
  permissions relative to some request.

  It provide an user, a set of groups and a set of roles.

  It is important to note that roles aren't associated to an user but to an
  IdentityInfo. This enable the same user to provide different roles via
  different API-Key for example.

  An IdentityInfo while having some mandatory informations could also contains
  some other informations generally for dealing with technical details and ease
  the debugging."
  (st/merge
   {:user User
    :groups #{Group}
    :roles #{Role}}
   {s/Keyword s/Any}))
```

Then the middleware will passe the ring request through all extractors and the
first return successful extractor will add an `:identity-info` field to the ring
request.

It is used that way:

```clojure
(def extractors [jwt-extractor api-key-extractor])

(let [app ((wrap-fn extractors) handler)]
  ...)
```

Where here are some example of extractors:

```clojure
;; Extractor code example for some JWT

(s/defn extract-identity-infos :- IdentityInfo
  [jwt-info]
  {:id {:id (:sub jwt-info)
        :name (:sub jwt-info)}
   :groups #{{:id (:org_guid jwt-info)
              :name (:org_name jwt-info)}}
   :roles (if (= "true"
                 ;; this test handle the case when :user_admin is a string
                 ;; and when its a boolean
                 (str (:user_admin jwt-info)))
            #{:admin :user}
            #{:user})
   :auth-type :jwt})

(s/defn jwt-extractor :- (s/maybe IdentityInfo)
  [req]
  (some-> req
          :jwt
          extract-identity-infos))

;; Extractor code example for API Key considering thay :api-key-info field
;; already contains an IdentityInfo

(s/defn api-key-extractor :- (s/maybe IdentityInfo)
  [req]
  (some-> req
          :api-key-infos
          (assoc :auth-type :api-key)))
```

Furthermore this middleware also provides the ability to destructure information
if you use compojure-api.
Typically you could:

~~~clojure
(GET "/foo" []
     :identity-info [id-info]
     (... do something with id-info ...))
~~~

and also

~~~clojure
 ;; only user with the role :admin could access this route
(GET "/foo" []
     :roles-filter #{:admin}
     ...)
~~~

## License

Copyright © 2017 Cisco

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

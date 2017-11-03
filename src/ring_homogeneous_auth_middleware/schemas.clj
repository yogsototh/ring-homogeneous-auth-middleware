(ns ring-homogeneous-auth-middleware.schemas
  (:require [schema.core :as s]
            [schema-tools.core :as st]))


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

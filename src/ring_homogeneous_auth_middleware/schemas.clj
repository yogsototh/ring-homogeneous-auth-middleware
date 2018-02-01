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
   {:id s/Str}
   ;; could contain other meta datas (name, nickname, email, address, phone number, etc...)
   {s/Keyword s/Any}))

(s/defschema Org
  "An Org can be understood as a Community of People, an Organization, a
  Business, etc... This also can be thought as a UNIX group.

  Mainly this should provide a way to filter document for an organization.

  A group must have an unique identifier and a name.

  A group could also have some meta informations. For example, a physical
  address, an Identity Provider URL, etc.."
  (st/merge
   {:id s/Str}
   ;; could contain other meta datas (name, Identity Provider URL, etc...)
   {s/Keyword s/Any}))

(def Scope
  "The scope of an user.

  The scope is a string without any space.
  Mainly this should provide a way to filter route access.

  Typical values are:

  - \"admin\"
  - \"service\"
  - \"service/subservice:read-only\"

  etc..."
  s/Str)

(s/defschema IdentityInfo
  "An IdentityInfo provide the information to identify and determine the
  permissions relative to some request.

  It provide an user, a main org and a set of scopes.

  It is important to note that scopes aren't associated to an user but to an
  IdentityInfo. This enable the same user to provide different scopes via
  different API-Key for example.

  An IdentityInfo while having some mandatory informations could also contains
  some other informations generally for dealing with technical details and ease
  the debugging.

  But they could also be used to extend the actual spec.
  For example, we could imagine that we might want to associate a set of orgs
  to an identity.

  But that's out of the scope of this specific spec."
  (st/merge
   {:user   User
    :org    Org
    :scopes #{Scope}}
   {s/Keyword s/Any}))

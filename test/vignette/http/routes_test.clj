(ns vignette.http.routes-test
  (:require [vignette.media-types :as mt]
            [vignette.http.routes :refer :all]
            [vignette.storage.protocols :refer :all]
            [vignette.protocols :refer :all]
            [vignette.util.thumbnail :as u]
            [midje.sweet :refer :all]
            [clout.core :refer (route-compile route-matches)]
            [ring.mock.request :refer :all]
            [clojure.java.io :as io])
  (:import java.io.FileNotFoundException))

(facts :original-route
  (route-matches original-route (request :get "/swift/v1")) => falsey
  (route-matches
    original-route
    (request :get "/lotr/3/35/Arwen_Sword.PNG")) => (contains {:wikia "lotr"
                                                               :top-dir "3"
                                                               :middle-dir "35"
                                                               :original "Arwen_Sword.PNG"}))

(facts :thumbnail-route
  (route-matches thumbnail-route (request :get "something")) => falsey
  (route-matches thumbnail-route
                 (request :get "/lotr/3/35/Arwen_Sword.PNG/resize/250/250")) => (contains {:wikia "lotr"
                                                                                           :top-dir "3"
                                                                                           :middle-dir "35"
                                                                                           :original "Arwen_Sword.PNG"
                                                                                           :mode "resize"
                                                                                           :width "250"
                                                                                           :height "250"
                                                                                           }))
(facts :app-routes
  ((app-routes nil) (request :get "/not-a-valid-route")) => (contains {:status 404}))

(facts :app-routes-thumbnail
  (let [route-params {:type :thumbnail, :original "ropes.jpg", :middle-dir "35", :top-dir "3", :wikia "lotr" :mode "resize" :height "10" :width "10"}]
    ((app-routes ..system..) (request :get "/lotr/3/35/ropes.jpg/resize/10/10")) => (contains {:status 200})
    (provided
     (mt/get-media-map route-params) => route-params
     (u/get-or-generate-thumbnail ..system.. route-params) => (io/file "image-samples/ropes.jpg"))

    ((app-routes ..system..) (request :get "/lotr/3/35/ropes.jpg/resize/10/10")) => (contains {:status 404})
    (provided
     (mt/get-media-map route-params) => route-params
     (u/get-or-generate-thumbnail ..system.. route-params) => nil)

    ((app-routes ..system..) (request :get "/lotr/3/35/ropes.jpg/resize/10/10")) => (contains {:status 503})
    (provided
      (mt/get-media-map route-params) => route-params
      (u/get-or-generate-thumbnail ..system.. route-params) => (throws java.io.FileNotFoundException))))

(facts :app-routes-original

  (let [route-params {:type :original, :original "ropes.jpg", :middle-dir "35", :top-dir "3", :wikia "lotr"} ]
    ((app-routes ..system..) (request :get "/lotr/3/35/ropes.jpg")) => (contains {:status 200})
    (provided
     (store ..system..) => ..store..
     (mt/get-media-map route-params) => route-params
     (get-original ..store.. route-params) => (io/file "image-samples/ropes.jpg"))

    ((app-routes ..system..) (request :get "/lotr/3/35/ropes.jpg")) => (contains {:status 404})
    (provided
     (store ..system..) => ..store..
     (mt/get-media-map route-params) => route-params
     (get-original ..store.. route-params) => nil)

    ((app-routes ..system..) (request :get "/lotr/3/35/ropes.jpg")) => (contains {:status 503})
    (provided
      (store ..system..) => ..store..
      (mt/get-media-map route-params) => route-params
      (get-original ..store.. route-params) => (throws java.io.FileNotFoundException))))
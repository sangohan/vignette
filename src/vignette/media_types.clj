(ns vignette.media-types
  (:require [slingshot.slingshot :refer [throw+]]
            [vignette.util.query-options :refer :all]))

(declare original
         thumbnail
         thumb-map->path)

(def archive-dir "archive")

(defmulti image-type->path-prefix (fn [object-map] (:image-type object-map)))

(defmethod image-type->path-prefix "avatars" [object-map]
  (:image-type object-map))

(defmethod image-type->path-prefix "images" [object-map]
  (let [path-prefix (query-opt object-map :path-prefix)
        lang (query-opt object-map :lang)
        prefix (:image-type object-map)]
    (clojure.string/join "/" (filter not-empty [path-prefix lang prefix]))))

(defmethod image-type->path-prefix :default [object-map]
  (throw+ {:type :convert-error
           :image-type (:image-type object-map)}
          "unsupported image type"))

(defn thumb-map->prefix [object-map]
  (let [prefix (image-type->path-prefix object-map)]
    (str prefix "/thumb" )))

(defn revision
  [data]
  (if (= (:revision data) "latest")
    nil
    (:revision data)))

(defn revision-filename
  [data]
  (if-let [revision (revision data)]
    (str revision "!" (original data))
    (original data)))

(defn top-dir
  [data]
  (:top-dir data))

(defn middle-dir
  [data]
  (:middle-dir data))

(defn original
  [data]
  (:original data))

(defn original-path
  "From a request map, generate a URI for an original image. These typically
  take the form of one of the following:

  /bucket/images/a/ab/original.ext
  /bucket/avatars/a/ab/original.ext
  /bucket/lang/images/a/ab/original.ext
  /bucket/lang/images/timeline/original.ext
  "
  [data]
  (let [prefix (image-type->path-prefix data)
        image-path (clojure.string/join "/" (filter not-empty ((juxt top-dir middle-dir) data)))
        filename (revision-filename data)]
    (if (nil? (revision data))
      (clojure.string/join "/" [prefix image-path filename])
      (clojure.string/join "/" [prefix archive-dir image-path filename]))))

(defn wikia
  [data]
  (:wikia data))

(defn mode
  [data]
  (:thumbnail-mode data))

(defn window-format
  [x-or-y offset width-or-height]
  (if (and offset width-or-height)
    (str x-or-y "[offset=" offset ",length=" width-or-height "]")
    x-or-y))

(defn height
  [data]
  (let [height (if (keyword? (:height data))
                 (name (:height data))
                 (:height data))
        y-offset (:y-offset data)
        window-height (:window-height data)]
    (window-format height y-offset window-height)))

(defn width
  [data]
  (let [width (if (keyword? (:width data))
                (name (:width data))
                (:width data))
        x-offset (:x-offset data)
        window-width (:window-width data)]
    (window-format width x-offset window-width)))

(defn thumbnail-path
  [data]
  (let [thumb-path (clojure.string/join "/" ((juxt top-dir middle-dir) data))]
    (thumb-map->path data thumb-path)))

(defmulti thumb-map->path (fn [data image-path]
                          (revision data)))

(defmethod thumb-map->path nil [data image-path]
  (let [prefix (thumb-map->prefix data)
        name (format "%s/%spx-%spx-%s%s-%s" (original data) (width data) (height data) (mode data) (query-opts-str data) (original data))]
    (clojure.string/join "/" [prefix image-path name])))

(defmethod thumb-map->path :default [data image-path]
  (let [prefix (thumb-map->prefix data)
        name (format "%spx-%spx-%s%s-%s" (width data) (height data) (mode data) (query-opts-str data) (original data))]
    (clojure.string/join "/" [prefix archive-dir image-path (revision-filename data) name])))

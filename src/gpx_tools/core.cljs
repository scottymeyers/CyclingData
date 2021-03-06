(ns gpx-tools.core
  (:require
   [reagent.core :as r]
   [reagent.dom :as rdom]
   [gpx-tools.components :as component]
   [gpx-tools.map :as maptools]
   [gpx-tools.utilities :as util]))

(enable-console-print!)

(defonce app-state (r/atom {:error nil
                            :activities []
                            :selected-activity nil}))
(defonce gmap (r/atom nil))

(defn set-error
  "Stores Errors that should be displayed"
  [error]
  (reset! app-state {:error error}))

(defn select-activity [activity]
  (if (and (:selected-activity @app-state)
           (identical?
            (util/get-activity-time (:selected-activity @app-state))
            (util/get-activity-time activity)))
    (swap! app-state assoc :selected-activity nil)
    (do
      (swap! app-state assoc :selected-activity activity)
      (let [path (map maptools/lat-lng (util/get-activity-trkpts activity))]
        (maptools/set-map-boundary path gmap)))))

(defn store-activities
  [activities]
  (.then activities #(swap! app-state assoc-in [:activities] %)))

;; TODO: create a Map component
(defn setup-google-maps []
  (let [api-key (subs (-> js/document .-location .-search) 1)
        center (clj->js {"lat" 40.730610
                         "lng" -73.935242})
        loader (google.maps.plugins.loader.Loader.
                (clj->js {:apiKey api-key
                          :version "weekly"}))]
    (.addEventListener
     js/window
     "DOMContentLoaded"
     (-> (.load loader)
         (.then (fn []
                  (set! gmap (google.maps.Map.
                              (. js/document (getElementById "map"))
                              (clj->js
                               {:center center
                                :zoom 8
                                :fullscreenControl false
                                :clickableIcons false
                                :disableDoubleClickZoom true})))))
         (.catch #(set-error "Unable to load Google Maps"))))))
(.addEventListener js/window "load" setup-google-maps)

(defn app []
  (fn []
    [:div
     [component/error-message (:error @app-state) set-error]
     [component/file-importer "gpx" store-activities]
     [component/activities-list
      (:activities @app-state)
      (:selected-activity @app-state)
      select-activity]

     (doall (for [activity (:activities @app-state)]
              ^{:key (util/get-activity-time activity)}
              [maptools/activity
               activity
               gmap]))]))

(rdom/render [app]
             (. js/document (getElementById "app")))

;; TODO: adjust boundaries based on all activities
;; (maptools/set-map-boundary position)
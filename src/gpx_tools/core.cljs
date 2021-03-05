(ns gpx-tools.core
  (:require
   [reagent.core :as r]
   [reagent.dom :as rdom]
   [gpx-tools.components :as component]
   [gpx-tools.map :as maptools]
   [gpx-tools.utilities :as utilities]))

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
            (utilities/get-activity-time (:selected-activity @app-state))
            (utilities/get-activity-time activity)))
    (swap! app-state assoc :selected-activity nil)
    (swap! app-state assoc :selected-activity activity)))

(defn render-activities
  "Accepts a Promise and then associates activities in @app-state"
  [result]
  (.then result #(swap! app-state assoc :activities %)))

;; TODO: create a Map component
(defn setup-google-maps []
  (let [api-key (subs (-> js/document .-location .-search) 1)
        center (clj->js {"lat" 40.730610
                         "lng" -73.935242})
        loader (google.maps.plugins.loader.Loader.
                (clj->js {"apiKey" api-key
                          "version" "weekly"}))]
    (.addEventListener
     js/window
     "DOMContentLoaded"
     (-> (.load loader)
         (.then (fn []
                  (set! gmap (google.maps.Map.
                              (. js/document (getElementById "map"))
                              (js-obj
                               "center" center
                               "zoom" 8
                               "fullscreenControl" false
                               "clickableIcons" false
                               "disableDoubleClickZoom" false)))))
         (.catch #(set-error "Unable to load Google Maps"))))))

(.addEventListener js/window "load" setup-google-maps)

(defn app []
  [:div
   [component/file-importer
    "gpx"
    render-activities]

  ;; TODO: create map from :selected-activity GPX
   (when (:selected-activity @app-state)
     [:section
      [:h2 (utilities/get-activity-name (:selected-activity @app-state))]
      [:small (utilities/get-activity-time (:selected-activity @app-state))]])

   [component/error-message (:error @app-state) set-error]

   (doall (for [activity (:activities @app-state)]
            ^{:key (utilities/get-activity-time activity)}
            [maptools/polyline
             activity
             gmap
             select-activity
             (:selected-activity @app-state)]))])


(rdom/render [app]
             (. js/document (getElementById "app")))

;; TODO: adjust boundaries based on all activities
;; (set-map-boundary position)